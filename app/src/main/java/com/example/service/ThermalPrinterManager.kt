package com.example.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import com.example.data.model.MerchantProfile
import com.example.util.UpiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class PrinterPaperWidth(val displayName: String, val columns: Int, val maxPixelWidth: Int) {
    WIDTH_58MM("2-Inch (58mm)", 32, 384),
    WIDTH_80MM("3-Inch (80mm)", 48, 576)
}

enum class PrinterConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class DiscoveredPrinter(
    val name: String,
    val address: String,
    val isBonded: Boolean
)

class ThermalPrinterManager(private val context: Context) {

    companion object {
        private const val TAG = "ThermalPrinterManager"
        private const val PREFS_NAME = "vyapar_printer_prefs"
        private const val KEY_LAST_DEVICE_ADDRESS = "last_printer_address"
        private const val KEY_LAST_DEVICE_NAME = "last_printer_name"
        private const val KEY_PAPER_WIDTH = "printer_paper_width"

        // Standard SPP (Serial Port Profile) UUID for Bluetooth thermal printers
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        @Volatile
        private var instance: ThermalPrinterManager? = null

        fun getInstance(context: Context): ThermalPrinterManager {
            return instance ?: synchronized(this) {
                instance ?: ThermalPrinterManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow(PrinterConnectionState.DISCONNECTED)
    val connectionState: StateFlow<PrinterConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DiscoveredPrinter?>(null)
    val connectedDevice: StateFlow<DiscoveredPrinter?> = _connectedDevice.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredPrinter>> = _discoveredDevices.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var currentPaperWidth: PrinterPaperWidth
        get() {
            val name = prefs.getString(KEY_PAPER_WIDTH, PrinterPaperWidth.WIDTH_58MM.name)
            return try {
                PrinterPaperWidth.valueOf(name ?: PrinterPaperWidth.WIDTH_58MM.name)
            } catch (e: Exception) {
                PrinterPaperWidth.WIDTH_58MM
            }
        }
        set(value) {
            prefs.edit().putString(KEY_PAPER_WIDTH, value.name).apply()
        }

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    init {
        loadLastConnectedDevice()
    }

    private fun loadLastConnectedDevice() {
        val lastAddr = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null)
        val lastName = prefs.getString(KEY_LAST_DEVICE_NAME, null)
        if (!lastAddr.isNullOrBlank()) {
            _connectedDevice.value = DiscoveredPrinter(lastName ?: "Saved Printer", lastAddr, true)
        }
    }

    // ==========================================================
    // 1. Device Scanning & Pairing
    // ==========================================================
    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<DiscoveredPrinter> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices.map {
                DiscoveredPrinter(
                    name = it.name ?: "Unknown Printer",
                    address = it.address,
                    isBonded = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bonded devices: ${e.message}")
            emptyList()
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    if (device != null) {
                        val name = device.name ?: "Bluetooth Device"
                        val address = device.address
                        val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
                        val printer = DiscoveredPrinter(name, address, isBonded)

                        val current = _discoveredDevices.value.toMutableList()
                        if (current.none { it.address == address }) {
                            current.add(printer)
                            _discoveredDevices.value = current
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (_connectionState.value == PrinterConnectionState.SCANNING) {
                        _connectionState.value = PrinterConnectionState.DISCONNECTED
                        _statusMessage.value = "Scan finished. Found ${_discoveredDevices.value.size} devices."
                    }
                }
            }
        }
    }

    private var isReceiverRegistered = false

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter ?: run {
            _statusMessage.value = "Bluetooth not supported on this device"
            return
        }
        if (!adapter.isEnabled) {
            _statusMessage.value = "Bluetooth is turned off. Please enable it."
            return
        }

        try {
            if (!isReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                context.registerReceiver(discoveryReceiver, filter)
                isReceiverRegistered = true
            }

            _discoveredDevices.value = getBondedDevices()
            _connectionState.value = PrinterConnectionState.SCANNING
            _statusMessage.value = "Scanning for nearby thermal printers..."

            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan: ${e.message}")
            _statusMessage.value = "Scan error: ${e.message}"
            _connectionState.value = PrinterConnectionState.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            if (isReceiverRegistered) {
                context.unregisterReceiver(discoveryReceiver)
                isReceiverRegistered = false
            }
            if (_connectionState.value == PrinterConnectionState.SCANNING) {
                _connectionState.value = PrinterConnectionState.DISCONNECTED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping scan: ${e.message}")
        }
    }

    // ==========================================================
    // 2. Connection Management
    // ==========================================================
    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String, deviceName: String = "Thermal Printer"): Result<Boolean> = withContext(Dispatchers.IO) {
        stopScan()
        disconnect()

        val adapter = bluetoothAdapter ?: return@withContext Result.failure(Exception("Bluetooth adapter unavailable"))
        _connectionState.value = PrinterConnectionState.CONNECTING
        _statusMessage.value = "Connecting to $deviceName..."

        try {
            val device = adapter.getRemoteDevice(deviceAddress)
            val socket = try {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: Exception) {
                // Fallback reflection for devices with non-standard RFCOMM channels
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                m.invoke(device, 1) as BluetoothSocket
            }

            adapter.cancelDiscovery()
            socket.connect()

            activeSocket = socket
            outputStream = socket.outputStream

            _connectedDevice.value = DiscoveredPrinter(deviceName, deviceAddress, true)
            _connectionState.value = PrinterConnectionState.CONNECTED
            _statusMessage.value = "Connected to $deviceName"

            prefs.edit()
                .putString(KEY_LAST_DEVICE_ADDRESS, deviceAddress)
                .putString(KEY_LAST_DEVICE_NAME, deviceName)
                .apply()

            Log.d(TAG, "Successfully connected to printer: $deviceAddress ($deviceName)")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            disconnect()
            _connectionState.value = PrinterConnectionState.ERROR
            _statusMessage.value = "Connection failed: ${e.message}"
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing stream: ${e.message}")
        }
        try {
            activeSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing socket: ${e.message}")
        }
        activeSocket = null
        outputStream = null
        _connectionState.value = PrinterConnectionState.DISCONNECTED
    }

    // ==========================================================
    // 3. ESC/POS Command Builder & Printing
    // ==========================================================
    suspend fun printInvoice(invoice: InvoiceData, paperWidth: PrinterPaperWidth = currentPaperWidth): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream
        if (stream == null || activeSocket?.isConnected != true) {
            val lastAddr = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null)
            val lastName = prefs.getString(KEY_LAST_DEVICE_NAME, "Printer") ?: "Printer"
            if (!lastAddr.isNullOrBlank()) {
                val connRes = connect(lastAddr, lastName)
                if (connRes.isFailure) {
                    return@withContext Result.failure(Exception("Printer not connected. Please connect printer in settings."))
                }
            } else {
                return@withContext Result.failure(Exception("No thermal printer connected. Setup in Printer Settings."))
            }
        }

        try {
            val builder = EscPosBuilder(paperWidth)
            builder.init()

            // Centered Shop Header
            builder.alignCenter()
            builder.doubleHeightWidth(true)
            builder.bold(true)
            builder.println(invoice.merchantProfile.shopName.ifBlank { "VyaparSetu Store" })
            builder.doubleHeightWidth(false)
            builder.bold(false)

            if (invoice.merchantProfile.address.isNotBlank()) {
                builder.println(invoice.merchantProfile.address)
            }
            if (invoice.merchantProfile.phone.isNotBlank()) {
                builder.println("Tel: ${invoice.merchantProfile.phone}")
            }
            if (invoice.merchantProfile.gstNumber.isNotBlank()) {
                builder.bold(true)
                builder.println("GSTIN: ${invoice.merchantProfile.gstNumber}")
                builder.bold(false)
            }

            builder.println("--------------------------------".take(paperWidth.columns))
            builder.bold(true)
            builder.println(invoice.mode.title)
            builder.bold(false)
            builder.println("--------------------------------".take(paperWidth.columns))

            // Metadata Left-Right
            builder.alignLeft()
            val dateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(invoice.dateMillis))
            builder.printTwoColumns("Inv: ${invoice.invoiceNumber}", dateStr)

            if (invoice.customerName.isNotBlank()) {
                builder.printTwoColumns("Cust: ${invoice.customerName.take(16)}", invoice.customerPhone)
            }

            builder.println("--------------------------------".take(paperWidth.columns))

            // Item Headers
            builder.bold(true)
            if (paperWidth == PrinterPaperWidth.WIDTH_58MM) {
                // 32 chars: Item(14) Qty(4) Rate(6) Amt(8)
                builder.println("ITEM          QTY  RATE   TOTAL")
            } else {
                // 48 chars: Item(22) HSN(6) Qty(5) Rate(7) Total(8)
                builder.println("ITEM                  HSN    QTY   RATE    TOTAL")
            }
            builder.bold(false)
            builder.println("--------------------------------".take(paperWidth.columns))

            // Items
            for (item in invoice.items) {
                if (paperWidth == PrinterPaperWidth.WIDTH_58MM) {
                    val name = if (item.name.length > 13) item.name.take(12) + "." else item.name.padEnd(13)
                    val qty = "${item.quantity.toInt()}".padStart(4)
                    val rate = "${item.rate.toInt()}".padStart(6)
                    val total = String.format(Locale.US, "%.1f", item.totalAmount).padStart(8)
                    builder.println("$name $qty $rate $total")
                } else {
                    val name = if (item.name.length > 20) item.name.take(19) + "." else item.name.padEnd(20)
                    val hsn = (item.hsnCode.take(6)).padEnd(6)
                    val qty = "${item.quantity.toInt()}".padStart(5)
                    val rate = String.format(Locale.US, "%.1f", item.rate).padStart(7)
                    val total = String.format(Locale.US, "%.2f", item.totalAmount).padStart(8)
                    builder.println("$name $hsn $qty $rate $total")
                }
            }

            builder.println("--------------------------------".take(paperWidth.columns))

            // Totals
            val calc = invoice.calculation
            builder.printTwoColumns("Subtotal:", "₹${String.format(Locale.US, "%.2f", calc.subtotal)}")
            if (calc.totalDiscount > 0) {
                builder.printTwoColumns("Discount:", "-₹${String.format(Locale.US, "%.2f", calc.totalDiscount)}")
            }
            if (invoice.mode == InvoiceMode.GST_TAX_INVOICE) {
                if (invoice.isInterState) {
                    builder.printTwoColumns("IGST:", "₹${String.format(Locale.US, "%.2f", calc.igst)}")
                } else {
                    builder.printTwoColumns("CGST:", "₹${String.format(Locale.US, "%.2f", calc.cgst)}")
                    builder.printTwoColumns("SGST:", "₹${String.format(Locale.US, "%.2f", calc.sgst)}")
                }
            }
            if (calc.roundOff != 0.0) {
                builder.printTwoColumns("Round Off:", "₹${String.format(Locale.US, "%+.2f", calc.roundOff)}")
            }

            builder.println("================================".take(paperWidth.columns))
            builder.bold(true)
            builder.doubleHeightWidth(true)
            builder.printTwoColumns("TOTAL:", "₹${String.format(Locale.US, "%.2f", calc.finalAmount)}")
            builder.doubleHeightWidth(false)
            builder.bold(false)
            builder.println("================================".take(paperWidth.columns))

            builder.printTwoColumns("Payment Mode:", "${invoice.paymentMode} (${invoice.paymentStatus})")

            // UPI QR Code
            if (invoice.merchantProfile.upiId.isNotBlank()) {
                val upiUri = UpiHelper.createUpiUri(
                    upiId = invoice.merchantProfile.upiId,
                    merchantName = invoice.merchantProfile.shopName,
                    amount = calc.finalAmount,
                    note = "Bill ${invoice.invoiceNumber}"
                )
                val qrBmp = UpiHelper.generateQrBitmap(upiUri, 180)
                if (qrBmp != null) {
                    builder.alignCenter()
                    builder.println("Scan UPI QR to Pay")
                    builder.printRasterBitmap(qrBmp)
                    builder.println(invoice.merchantProfile.upiId)
                }
            }

            // Footer
            builder.alignCenter()
            builder.println("Thank you! Please visit again.")
            builder.println("VyaparSetu Smart POS Billing")
            builder.feed(3)
            builder.cut()

            val bytes = builder.toByteArray()
            outputStream?.write(bytes)
            outputStream?.flush()

            _statusMessage.value = "Invoice ${invoice.invoiceNumber} printed successfully"
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Print invoice error: ${e.message}")
            _statusMessage.value = "Print error: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun printTestReceipt(shopName: String = "VyaparSetu Store", paperWidth: PrinterPaperWidth = currentPaperWidth): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream
        if (stream == null || activeSocket?.isConnected != true) {
            return@withContext Result.failure(Exception("Printer is not connected"))
        }

        try {
            val builder = EscPosBuilder(paperWidth)
            builder.init()
            builder.alignCenter()
            builder.doubleHeightWidth(true)
            builder.bold(true)
            builder.println(shopName)
            builder.doubleHeightWidth(false)
            builder.bold(false)
            builder.println("TEST RECEIPT")
            builder.println(SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date()))
            builder.println("--------------------------------".take(paperWidth.columns))
            builder.alignLeft()
            builder.println("Paper Width: ${paperWidth.displayName}")
            builder.println("Columns: ${paperWidth.columns} cols")
            builder.println("Connection: Bluetooth SPP")
            builder.println("ESC/POS Commands: OK")
            builder.println("--------------------------------".take(paperWidth.columns))
            builder.alignCenter()
            builder.bold(true)
            builder.println("PRINTER TEST SUCCESSFUL!")
            builder.bold(false)
            builder.feed(3)
            builder.cut()

            outputStream?.write(builder.toByteArray())
            outputStream?.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Print test error: ${e.message}")
            Result.failure(e)
        }
    }

    // ==========================================================
    // 4. Low-Level ESC/POS Byte Command Builder
    // ==========================================================
    class EscPosBuilder(val paperWidth: PrinterPaperWidth) {
        private val buffer = ByteArrayOutputStream()

        fun init(): EscPosBuilder {
            // ESC @ (Initialize printer)
            buffer.write(byteArrayOf(0x1B, 0x40))
            return this
        }

        fun alignLeft(): EscPosBuilder {
            // ESC a 0
            buffer.write(byteArrayOf(0x1B, 0x61, 0x00))
            return this
        }

        fun alignCenter(): EscPosBuilder {
            // ESC a 1
            buffer.write(byteArrayOf(0x1B, 0x61, 0x01))
            return this
        }

        fun alignRight(): EscPosBuilder {
            // ESC a 2
            buffer.write(byteArrayOf(0x1B, 0x61, 0x02))
            return this
        }

        fun bold(enable: Boolean): EscPosBuilder {
            // ESC E n
            buffer.write(byteArrayOf(0x1B, 0x45, if (enable) 0x01 else 0x00))
            return this
        }

        fun doubleHeightWidth(enable: Boolean): EscPosBuilder {
            // GS ! n (0x11 = double width & height, 0x00 = normal)
            buffer.write(byteArrayOf(0x1D, 0x21, if (enable) 0x11 else 0x00))
            return this
        }

        fun println(text: String): EscPosBuilder {
            val bytes = (text + "\n").toByteArray(Charsets.UTF_8)
            buffer.write(bytes)
            return this
        }

        fun printTwoColumns(left: String, right: String): EscPosBuilder {
            val totalCols = paperWidth.columns
            val maxLeftLen = totalCols - right.length - 1
            val safeLeft = if (left.length > maxLeftLen) left.take(maxLeftLen) else left
            val spaces = " ".repeat((totalCols - safeLeft.length - right.length).coerceAtLeast(1))
            println(safeLeft + spaces + right)
            return this
        }

        fun feed(lines: Int = 2): EscPosBuilder {
            // ESC d n
            buffer.write(byteArrayOf(0x1B, 0x64, lines.toByte()))
            return this
        }

        fun cut(): EscPosBuilder {
            // GS V A 0 (Partial cut)
            buffer.write(byteArrayOf(0x1D, 0x56, 0x41, 0x00))
            return this
        }

        /**
         * Renders a monochrome bitmap using ESC/POS Raster Bit Image command (GS v 0)
         */
        fun printRasterBitmap(bitmap: Bitmap): EscPosBuilder {
            val width = bitmap.width
            val height = bitmap.height
            val widthBytes = (width + 7) / 8

            // GS v 0 m xL xH yL yH
            val xL = (widthBytes and 0xFF).toByte()
            val xH = ((widthBytes shr 8) and 0xFF).toByte()
            val yL = (height and 0xFF).toByte()
            val yH = ((height shr 8) and 0xFF).toByte()

            buffer.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

            val rasterData = ByteArray(widthBytes * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b)

                    if (luminance < 128) { // Black pixel
                        val byteIndex = y * widthBytes + (x / 8)
                        val bitIndex = 7 - (x % 8)
                        rasterData[byteIndex] = (rasterData[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                    }
                }
            }

            buffer.write(rasterData)
            return this
        }

        fun toByteArray(): ByteArray = buffer.toByteArray()
    }
}
