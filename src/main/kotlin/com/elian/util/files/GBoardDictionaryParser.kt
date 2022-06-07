package com.elian.util.files

import java.io.*

private const val FILE_HEADER = "# Gboard Dictionary version:1"
private const val FILE_WITH_CATEGORIES_HEADER = "# Gboard Dictionary with categories version:1"

// This character is not a regular space character.
private const val SEPARATOR = "	"

private val languageCodeRegex = "^[a-z]{2}(-[A-Z]{2})?\$".toRegex()


object GBoardDictionaryParser
{
    @JvmStatic
    fun getAllWords(filepath: String): List<GBoardWord>
    {
        var line: String?

        val words = mutableListOf<GBoardWord>()

        BufferedReader(FileReader(filepath)).use { br ->

            // skips the file header
            if (br.readLine() != FILE_HEADER) throw NotAGBoardDictionaryFileException()

            while (br.readLine().also { line = it } != null)
            {
                val currentLine = line!!.split(SEPARATOR)

                val currentWord = GBoardWord(currentLine)

                words.add(currentWord)
            }
        }

        return words
    }

    @JvmStatic
    fun getAllWordsWithCategory(filepath: String): List<GBoardWord>
    {
        var line: String?

        val words = mutableListOf<GBoardWord>()

        BufferedReader(FileReader(filepath)).use { br ->

            // skips the file header
            if (br.readLine() != FILE_WITH_CATEGORIES_HEADER) throw NotAGBoardDictionaryWithCategoriesFileException()

            while (br.readLine().also { line = it } != null)
            {
                val currentLine = line!!.split(SEPARATOR)

                val currentWord = GBoardWord(currentLine).apply { category = currentLine[3] }

                words.add(currentWord)
            }
        }

        return words
    }

    @JvmStatic
    fun getWord(key: String, filepath: String): GBoardWord?
    {
        var line: String?

        BufferedReader(FileReader(filepath)).use { br ->

            // skips the file header
            if (br.readLine() != FILE_HEADER) throw NotAGBoardDictionaryFileException()

            while (br.readLine().also { line = it } != null)
            {
                val currentLine = line!!.split(SEPARATOR)

                val currentWord = GBoardWord(currentLine)

                if (currentWord.key == key)
                {
                    return currentWord
                }
            }
        }

        return null
    }

    @JvmStatic
    fun getWordWithCategory(key: String, filepath: String): GBoardWord?
    {
        var line: String?

        BufferedReader(FileReader(filepath)).use { br ->

            // skips the file header
            if (br.readLine() != FILE_WITH_CATEGORIES_HEADER) throw NotAGBoardDictionaryWithCategoriesFileException()

            while (br.readLine().also { line = it } != null)
            {
                val currentLine = line!!.split(SEPARATOR)

                val currentWord = GBoardWord(currentLine).apply { category = currentLine[3] }

                if (currentWord.key == key)
                {
                    return currentWord
                }
            }
        }

        return null
    }

    @JvmStatic
    fun saveAllWords(words: List<GBoardWord>, filepath: String)
    {
        // Words must be sorted by value before saving them, that's how GBoard dictionary works.
        val sortedWords = words.sortedBy { it.value }

        BufferedWriter(FileWriter(filepath)).use { bw ->

            bw.write(FILE_HEADER)
            bw.newLine()

            sortedWords.forEach {
                bw.write(it.toGBoardDictionary())
                bw.newLine()
            }
        }
    }

    /**
     * This method will be used when saving the words with its category
     * but, you can't use it for a regular GBoard dictionary file.
     */
    @JvmStatic
    fun saveAllWordsWithCategory(words: List<GBoardWord>, filepath: String)
    {
        // Words must be sorted by value before saving them, that's how GBoard dictionary works.
        val sortedWords = words.sortedBy { it.value }

        BufferedWriter(FileWriter(filepath)).use { bw ->

            bw.write(FILE_WITH_CATEGORIES_HEADER)
            bw.newLine()

            sortedWords.forEach {
                bw.write(it.toGBoardDictionaryWithCategory())
                bw.newLine()
            }
        }
    }

    /**
     * Inserts the word in the appropriate place in the dictionary.
     */
    @JvmStatic
    fun insertWord(word: GBoardWord, filepath: String)
    {
        val words = getAllWords(filepath) as MutableList<GBoardWord>

        words.add(word)

        val sortedWords = words.sortedBy { it.value }

        saveAllWords(sortedWords, filepath)
    }

    /**
     * Inserts the word in the appropriate place in the dictionary.
     */
    @JvmStatic
    fun insertWordWithCategory(word: GBoardWord, filepath: String)
    {
        val words = getAllWordsWithCategory(filepath) as MutableList<GBoardWord>

        words.add(word)

        val sortedWords = words.sortedBy { it.value }

        saveAllWords(sortedWords, filepath)
    }

    @JvmStatic
    fun deleteWord(word: GBoardWord, filepath: String)
    {
        val words = getAllWords(filepath)

        val newWords = words.filter { it.key != word.key }

        saveAllWords(newWords, filepath)
    }

    @JvmStatic
    fun deleteWordWithCategory(word: GBoardWord, filepath: String)
    {
        val words = getAllWordsWithCategory(filepath)

        val newWords = words.filter { it.key != word.key }

        saveAllWords(newWords, filepath)
    }

    @JvmStatic
    fun getCategories(filepath: String): Set<String>
    {
        val words = getAllWordsWithCategory(filepath)

        val categories = mutableSetOf<String>()

        words.forEach { categories.add(it.category) }

        return categories
    }
}

data class GBoardWord @JvmOverloads constructor(
    var key: String,
    var value: String,

    /**
     * ISO 639-1 language code.
     */
    var languageCode: String = "",

    /**
     * Extra property to group words.
     */
    var category: String = "",
)
{
    init
    {
        if (key.isEmpty()) throw KeyEmptyException(value)
        if (value.isEmpty()) throw ValueEmptyException(key)
        if (languageCode.isNotEmpty() && !languageCode.matches(languageCodeRegex)) throw IllegalLanguageCodeFormatException(languageCode)

        if (key.length > MAX_LENGTH) key = key.take(MAX_LENGTH)
        if (value.length > MAX_LENGTH) value = value.take(MAX_LENGTH)
    }

    constructor(fileLine: List<String>) : this(fileLine[0], fileLine[1], fileLine[2])

    fun toGBoardDictionary(): String = "$key$SEPARATOR$value$SEPARATOR$languageCode"

    fun toGBoardDictionaryWithCategory(): String = "$key$SEPARATOR$value$SEPARATOR$languageCode$SEPARATOR$category"

    companion object
    {
        // This is the maximum length of the key and value supported in the GBoard dictionary.
        private const val MAX_LENGTH = 100

        @JvmStatic
        fun groupWordsByCategory(words: List<GBoardWord>): Map<String, List<GBoardWord>>
        {
            val groupedWords = mutableMapOf<String, MutableList<GBoardWord>>()

            words.forEach {

                val currentCategory = it.category

                if (groupedWords.containsKey(currentCategory))
                {
                    groupedWords[currentCategory]?.add(it)
                }
                else groupedWords[currentCategory] = mutableListOf(it)
            }

            return groupedWords
        }
    }
}

class KeyEmptyException(value: String) : Exception("The key with value '$value' can't be empty.")
class ValueEmptyException(key: String) : Exception("The value with key '$key' can't be empty.")
class IllegalLanguageCodeFormatException(languageCode: String) : Exception("The language code '$languageCode' is not in the correct format")

class NotAGBoardDictionaryFileException : Exception("The file is not a GBoard dictionary file.")
class NotAGBoardDictionaryWithCategoriesFileException : Exception("The file is not a GBoard dictionary with categories file.")