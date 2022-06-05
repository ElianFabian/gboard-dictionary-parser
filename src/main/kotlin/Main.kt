import com.elian.util.files.GBoardDictionaryParser
import com.elian.util.files.GBoardDictionaryRecord

fun main()
{
    val filepath = "src/main/resources/dictionary_1.txt"
    
    val a = GBoardDictionaryRecord("cian", "#00FFFF", "es-ES")
    val b = GBoardDictionaryRecord("negro", "#000000", "es-ES")
    val c = GBoardDictionaryRecord("blanco", "#FFFFFF", "es-ES")

    val recordsToSave = listOf(a, b, c)
    
    val d = GBoardDictionaryRecord("azul", "#0000FF", "es-ES")

    GBoardDictionaryParser.insertRecord(d, filepath)
}