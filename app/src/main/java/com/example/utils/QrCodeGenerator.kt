package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object QrCodeGenerator {

    /**
     * Generates a clean QR code bitmap natively.
     * Uses a self-contained QR matrix encoder supporting versions 1-6 with Byte mode and EC Level M.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Color = Color.Black,
        backgroundColor: Color = Color.White,
        roundCorners: Boolean = true
    ): Bitmap {
        val safeText = if (content.isBlank()) "https://ai.studio" else content
        val matrix = SimpleQrEncoder.encode(safeText)
        val matrixSize = matrix.size

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor.toArgb()
            style = Paint.Style.FILL
        }
        // Draw background
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)

        // Module size
        val quietZoneModules = 2
        val totalModules = matrixSize + quietZoneModules * 2
        val moduleSize = sizePx.toFloat() / totalModules

        paint.color = foregroundColor.toArgb()
        val cornerRadius = if (roundCorners) moduleSize * 0.35f else 0f

        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (matrix[y][x]) {
                    val left = (x + quietZoneModules) * moduleSize
                    val top = (y + quietZoneModules) * moduleSize
                    val right = left + moduleSize
                    val bottom = top + moduleSize

                    if (roundCorners) {
                        canvas.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, paint)
                    } else {
                        canvas.drawRect(left, top, right, bottom, paint)
                    }
                }
            }
        }

        return bitmap
    }
}

/**
 * Compact standalone QR Code Matrix Generator (Byte mode, standard patterns)
 */
internal object SimpleQrEncoder {

    // Version definitions (version 1: 21, version 2: 25, version 3: 29, version 4: 33)
    fun encode(text: String): Array<BooleanArray> {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        // Max payload for single block version 7 (124 data codewords minus 2 for mode and count)
        val maxSafeBytes = 120
        val bytes = if (rawBytes.size > maxSafeBytes) rawBytes.copyOf(maxSafeBytes) else rawBytes

        val version = when {
            bytes.size <= 14 -> 1
            bytes.size <= 26 -> 2
            bytes.size <= 42 -> 3
            bytes.size <= 62 -> 4
            bytes.size <= 84 -> 5
            bytes.size <= 106 -> 6
            else -> 7
        }

        val size = 17 + 4 * version
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // 1. Finder patterns (top-left, top-right, bottom-left)
        drawFinderPattern(matrix, reserved, 0, 0)
        drawFinderPattern(matrix, reserved, size - 7, 0)
        drawFinderPattern(matrix, reserved, 0, size - 7)

        // 2. Timing patterns
        for (i in 8 until size - 8) {
            val bit = (i % 2 == 0)
            matrix[6][i] = bit
            reserved[6][i] = true
            matrix[i][6] = bit
            reserved[i][6] = true
        }

        // 3. Alignment patterns for version >= 2
        if (version >= 2) {
            val alignPos = when (version) {
                2 -> intArrayOf(6, 18)
                3 -> intArrayOf(6, 22)
                4 -> intArrayOf(6, 26)
                5 -> intArrayOf(6, 30)
                6 -> intArrayOf(6, 34)
                else -> intArrayOf(6, 22, 38)
            }
            for (r in alignPos) {
                for (c in alignPos) {
                    if (!reserved[r][c]) {
                        drawAlignmentPattern(matrix, reserved, r - 2, c - 2)
                    }
                }
            }
        }

        // 4. Reserve format info
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
        }
        for (i in 0..7) {
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }
        reserved[size - 8][8] = true // Dark module

        // 5. Build data bits with Byte mode (0100) + length + payload + terminator
        val dataBits = mutableListOf<Int>()
        // Mode indicator: 0100 (Byte)
        dataBits.addAll(listOf(0, 1, 0, 0))
        // Character count indicator (8 bits for versions 1-9 in byte mode)
        for (i in 7 downTo 0) {
            dataBits.add((bytes.size shr i) and 1)
        }
        // Data bytes
        for (b in bytes) {
            val unsigned = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                dataBits.add((unsigned shr i) and 1)
            }
        }
        // Terminator (up to 4 zeroes)
        val capacityBytes = getTotalDataCodewords(version)
        val capacityBits = capacityBytes * 8
        val termLen = (capacityBits - dataBits.size).coerceIn(0, 4)
        repeat(termLen) { dataBits.add(0) }
        // Pad to byte multiple
        while (dataBits.size % 8 != 0) {
            dataBits.add(0)
        }
        // Pad bytes (0xEC, 0x11)
        val pad = intArrayOf(0xEC, 0x11)
        var padIndex = 0
        while (dataBits.size < capacityBits) {
            val padByte = pad[padIndex % 2]
            for (i in 7 downTo 0) {
                dataBits.add((padByte shr i) and 1)
            }
            padIndex++
        }

        // Convert data bits to codewords
        val dataCodewords = IntArray(capacityBytes)
        for (i in 0 until capacityBytes) {
            var byteVal = 0
            for (b in 0..7) {
                val bitIdx = i * 8 + b
                val bit = if (bitIdx < dataBits.size) dataBits[bitIdx] else 0
                byteVal = (byteVal shl 1) or bit
            }
            dataCodewords[i] = byteVal
        }

        // Error correction codewords
        val ecCodewordsCount = getEcCodewordsCount(version)
        val allCodewords = generateCodewordsWithEc(dataCodewords, ecCodewordsCount)

        // Convert all codewords to bit stream
        val fullBitStream = mutableListOf<Int>()
        for (cw in allCodewords) {
            for (b in 7 downTo 0) {
                fullBitStream.add((cw shr b) and 1)
            }
        }

        // 6. Place data bits in matrix (right-to-left 2-column zigzag)
        var bitIndex = 0
        var upward = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col-- // skip vertical timing column

            val rows = if (upward) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (c in 0..1) {
                    val targetCol = col - c
                    if (!reserved[row][targetCol]) {
                        val bit = if (bitIndex < fullBitStream.size) fullBitStream[bitIndex++] else 0
                        // Apply Mask Pattern 0: (row + col) % 2 == 0
                        val mask = ((row + targetCol) % 2 == 0)
                        matrix[row][targetCol] = if (mask) bit == 0 else bit == 1
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // 7. Format info for Mask 0 and EC Level M (101010000010010)
        val formatBits = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0)
        // Draw top-left
        val formatCoordsTL = arrayOf(
            Pair(8, 0), Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5),
            Pair(8, 7), Pair(8, 8), Pair(7, 8), Pair(5, 8), Pair(4, 8), Pair(3, 8),
            Pair(2, 8), Pair(1, 8), Pair(0, 8)
        )
        for (i in 0..14) {
            val (r, c) = formatCoordsTL[i]
            matrix[r][c] = formatBits[i] == 1
        }

        // Draw around other corners
        for (i in 0..6) {
            matrix[size - 1 - i][8] = formatBits[i] == 1
        }
        for (i in 7..14) {
            matrix[8][size - 15 + i] = formatBits[i] == 1
        }
        // Dark module
        matrix[size - 8][8] = true

        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, r: Int, c: Int) {
        for (dr in -1..7) {
            for (dc in -1..7) {
                val row = r + dr
                val col = c + dc
                if (row in matrix.indices && col in matrix.indices) {
                    reserved[row][col] = true
                    val isOuterBorder = dr in 0..6 && (dc == 0 || dc == 6) || dc in 0..6 && (dr == 0 || dr == 6)
                    val isInnerBox = dr in 2..4 && dc in 2..4
                    matrix[row][col] = isOuterBorder || isInnerBox
                }
            }
        }
    }

    private fun drawAlignmentPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, r: Int, c: Int) {
        for (dr in 0..4) {
            for (dc in 0..4) {
                reserved[r + dr][c + dc] = true
                val isBorder = dr == 0 || dr == 4 || dc == 0 || dc == 4
                val isCenter = dr == 2 && dc == 2
                matrix[r + dr][c + dc] = isBorder || isCenter
            }
        }
    }

    private fun getTotalDataCodewords(version: Int): Int = when (version) {
        1 -> 16
        2 -> 28
        3 -> 44
        4 -> 64
        5 -> 86
        6 -> 108
        else -> 124
    }

    private fun getEcCodewordsCount(version: Int): Int = when (version) {
        1 -> 10
        2 -> 16
        3 -> 26
        4 -> 36
        5 -> 48
        6 -> 64
        else -> 72
    }

    // Standard Galois Field GF(256) Reed-Solomon encoding
    private fun generateCodewordsWithEc(data: IntArray, ecCount: Int): IntArray {
        val gfExp = IntArray(512)
        val gfLog = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfExp[i + 255] = x
            gfLog[x] = i
            x = (x shl 1)
            if (x >= 256) x = x xor 285
        }

        fun gfMul(a: Int, b: Int): Int {
            if (a == 0 || b == 0) return 0
            return gfExp[gfLog[a] + gfLog[b]]
        }

        // Generator polynomial for ecCount
        var genPoly = intArrayOf(1)
        for (i in 0 until ecCount) {
            val nextFactor = intArrayOf(1, gfExp[i])
            val newPoly = IntArray(genPoly.size + 1)
            for (j in genPoly.indices) {
                newPoly[j] = newPoly[j] xor gfMul(genPoly[j], nextFactor[0])
                newPoly[j + 1] = newPoly[j + 1] xor gfMul(genPoly[j], nextFactor[1])
            }
            genPoly = newPoly
        }

        // Polynomial division
        val remainder = IntArray(ecCount)
        for (b in data) {
            val factor = b xor remainder[0]
            for (j in 0 until ecCount - 1) {
                remainder[j] = remainder[j + 1] xor gfMul(genPoly[j + 1], factor)
            }
            remainder[ecCount - 1] = gfMul(genPoly[ecCount], factor)
        }

        val result = IntArray(data.size + ecCount)
        System.arraycopy(data, 0, result, 0, data.size)
        System.arraycopy(remainder, 0, result, data.size, ecCount)
        return result
    }
}
