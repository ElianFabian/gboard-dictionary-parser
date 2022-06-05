package com.elian.util.files

import java.io.*

private const val FILE_HEADER = "# Gboard Dictionary version:1"

// This character is not a regular space character.
private const val SEPARATOR = "	"

private val languageCodeRegex = "^[a-z]{2}(-[A-Z]{2})?\$".toRegex()


object GBoardDictionaryParser
{
    @JvmStatic
    fun getAllRecords(filepath: String): List<GBoardDictionaryRecord>
    {
        var line: String?

        val records = mutableListOf<GBoardDictionaryRecord>()

        BufferedReader(FileReader(filepath)).use { br ->

            br.readLine() // skip the file header

            while (br.readLine().also { line = it } != null)
            {
                val currentLine = line!!.split(SEPARATOR)

                val currentRecord = GBoardDictionaryRecord(currentLine)

                records.add(currentRecord)
            }
        }

        return records
    }

    @JvmStatic
    fun getRecord(filepath: String, key: String): GBoardDictionaryRecord?
    {
        var line: String?

        BufferedReader(FileReader(filepath)).use { br ->

            br.readLine() // skip the file header

            while (br.readLine().also { line = it } != null)
            {
                val currentLine = line!!.split(SEPARATOR)

                val currentRecord = GBoardDictionaryRecord(currentLine)

                if (currentRecord.key == key)
                {
                    return currentRecord
                }
            }
        }

        return null
    }

    @JvmStatic
    fun saveRecords(records: List<GBoardDictionaryRecord>, filepath: String)
    {
        // Records must be sorted by value before saving them, that's how GBoard dictionary works.
        val sortedRecords = records.sortedBy { it.value }

        BufferedWriter(FileWriter(filepath)).use { bw ->

            bw.write(FILE_HEADER)
            bw.newLine()

            sortedRecords.forEach {
                bw.write(it.toGBoardDictionary())
                bw.newLine()
            }
        }
    }

    /**
     * This method will be used when saving the records with its category
     * but, you can't use it for a regular GBoard dictionary file.
     */
    @JvmStatic
    fun saveRecordsWithCategory(records: List<GBoardDictionaryRecord>, filepath: String)
    {
        // Records must be sorted by value before saving them, that's how GBoard dictionary works.
        val sortedRecords = records.sortedBy { it.value }

        BufferedWriter(FileWriter(filepath)).use { bw ->

            bw.write(FILE_HEADER)
            bw.newLine()

            sortedRecords.forEach {
                bw.write(it.toGBoardDictionaryWithCategory())
                bw.newLine()
            }
        }
    }

    /**
     * Inserts the record in the appropriate place in the dictionary.
     */
    @JvmStatic
    fun insertRecord(record: GBoardDictionaryRecord, filepath: String)
    {
        val records = getAllRecords(filepath) as MutableList<GBoardDictionaryRecord>

        records.add(record)

        val sortedRecords = records.sortedBy { it.value }

        saveRecords(sortedRecords, filepath)
    }

    @JvmStatic
    fun deleteRecord(record: GBoardDictionaryRecord, filepath: String)
    {
        val records = getAllRecords(filepath)

        val newRecords = records.filter { it.key != record.key }

        saveRecords(newRecords, filepath)
    }
}

data class GBoardDictionaryRecord @JvmOverloads constructor(
    var key: String,
    var value: String,

    /**
     * ISO 639-1 language code.
     */
    var languageCode: String = "",

    /**
     * Extra property to group records.
     */
    var category: String = "",
)
{
    init
    {
        if (key.isEmpty()) throw ValueEmptyException(value)
        if (value.isEmpty()) throw ValueEmptyException(key)
        if (languageCode.isNotEmpty() && !languageCode.matches(languageCodeRegex)) throw IllegalLanguageCodeFormatException(languageCode)
    }

    constructor(fileLine: List<String>) : this(fileLine[0], fileLine[1], fileLine[2])

    fun toGBoardDictionary(): String = "$key$SEPARATOR$value$SEPARATOR$languageCode"

    fun toGBoardDictionaryWithCategory(): String = "$key$SEPARATOR$value$SEPARATOR$languageCode$SEPARATOR$category"

    companion object
    {
        @JvmStatic
        fun groupRecordsByCategory(records: List<GBoardDictionaryRecord>): Map<String, List<GBoardDictionaryRecord>>
        {
            val groupedRecords = mutableMapOf<String, MutableList<GBoardDictionaryRecord>>()

            records.forEach {

                val currentCategory = it.category

                if (groupedRecords.containsKey(currentCategory))
                {
                    groupedRecords[currentCategory]?.add(it)
                }
                else groupedRecords[currentCategory] = mutableListOf(it)
            }

            return groupedRecords
        }
    }
}

class KeyEmptyException(value: String) : Exception("The key with value '$value' can't be empty.")
class ValueEmptyException(key: String) : Exception("The value with key '$key' can't be empty.")
class IllegalLanguageCodeFormatException(languageCode: String) : Exception("The language code '$languageCode' is not in the correct format")