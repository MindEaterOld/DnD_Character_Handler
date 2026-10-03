package com.dndcharacterhandler.data.dice

import com.dndcharacterhandler.domain.model.CustomDiceSkin
import com.dndcharacterhandler.domain.model.DicePattern
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DiceSkinArchiveTest {
    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, data) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private fun read(bytes: ByteArray) = readSkinEntries(ByteArrayInputStream(bytes))

    @Test
    fun aSharedSkinReadsBackAsItWasWritten() {
        val skin = CustomDiceSkin(
            id = "skin-1",
            name = "Nebula",
            bodyColor = 0xFF141030.toInt(),
            edgeColor = 0xFF8080FF.toInt(),
            numberColor = 0xFFFFFFFF.toInt(),
            pattern = DicePattern.Nebula(0xFF303080.toInt(), 0xFFFFD86B.toInt(), 7),
            faceArt = setOf("D20")
        )
        val picture = byteArrayOf(1, 2, 3)
        val face = byteArrayOf(4, 5)
        val file = ByteArrayOutputStream()
        writeSkinEntries(file, mapOf(SKIN_ENTRY to encodeDiceSkins(listOf(skin)).toByteArray(), PICTURE_ENTRY to picture, faceEntry("D20") to face))

        val entries = read(file.toByteArray())!!
        assertEquals(listOf(skin), decodeDiceSkins(String(entries.getValue(SKIN_ENTRY))))
        assertArrayEquals(picture, entries.getValue(PICTURE_ENTRY))
        assertArrayEquals(face, entries.getValue(faceEntry("D20")))
    }

    @Test
    fun onlyTheKnownEntriesAreRead() {
        val entries = read(
            zipOf(
                SKIN_ENTRY to byteArrayOf(1),
                "../$PICTURE_ENTRY" to byteArrayOf(2),
                "folder/$SKIN_ENTRY" to byteArrayOf(3),
                "faces_d20.png" to byteArrayOf(4),
                "faces_D20.png.exe" to byteArrayOf(5),
                "faces_D10_TENS.png" to byteArrayOf(6)
            )
        )!!
        assertEquals(setOf(SKIN_ENTRY, "faces_D10_TENS.png"), entries.keys)
    }

    @Test
    fun aFileTooBigOrTooCrowdedIsNoSkin() {
        assertNull(read(zipOf(PICTURE_ENTRY to ByteArray(MAX_ENTRY_BYTES + 1))))
        val crowded = (0..MAX_ENTRIES).map { "junk_$it.txt" to byteArrayOf(0) }.toTypedArray()
        assertNull(read(zipOf(*crowded)))
    }
}
