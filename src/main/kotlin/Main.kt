package games.nextturn

import java.io.File
import java.lang.RuntimeException

var allWords = setOf<String>()
var commonWords = setOf<String>()

fun main() {
    loadWords()

    println("Available choices:")
    println("[1] Input grades (use as aide)")
    println("[2] Test against random word")
    println("[3] Test against input word")
    println("[4] Find best first guess")
    println("[5] Build cheat sheet")
    println("[6] Test all words")
    print("Enter choice > ")

    val highSelection = 6

    var menuChoice = 0
    while (menuChoice < 1 || menuChoice > highSelection) {
        try {
            menuChoice = readLine()?.toInt() ?: 0
        } catch (ex: Exception) {
            println("Please make a selection between 1 and $highSelection")
        }
    }

    when (menuChoice) {
        1 -> runAide()
        2 -> runTest(null)
        3 -> testAnswer()
        4 -> bestFirstGuess()
        5 -> buildCheatSheet()
        6 -> testAllWords()
    }
}

fun runAide() {
    val remaining = allWords.toMutableSet()

    var tries = 0
    var guess = "lares"
    var grade = 1

    while (grade != 0 && remaining.size > 0) {
        println("There are still ${remaining.size} possible words left")

        tries++

        var gradeIn = ""
        println("Guessing $guess")
        while (gradeIn.length != 5 && gradeIn.filter { listOf('.', 'c', 'W').contains(it) }.length != 5) {
            print("  Grade? > ")
            gradeIn = readLine() ?: ""

            if (gradeIn == "p") {
                for (w in remaining) {
                    println(w)
                }
            }

            if (gradeIn.length != 5 && gradeIn.filter { listOf('.', 'c', 'W').contains(it) }.length != 5) {
                println("Grade must be 5 characters. Use '.' for not in answer, 'w' for wrong sport and 'C' for Correct")
            }
        }

        grade = stringToGrade(gradeIn)

        remaining.removeIf { !checkWord(guess, it, grade) }
        guess = bestGuess(remaining)
    }

    println("Word is $guess, took $tries tries")
}

fun runTest(input: String?) {
    val answer = input ?: allWords.random()
    println("Testing for the answer $answer")

    val remaining = allWords.toMutableSet()

    var tries = 1
    var guess = "lares"

    while (guess != answer && remaining.size > 0) {
        tries++
        val grade = gradeWord(guess, answer)
        println("Guessing $guess, scored a ${prettyGrade(grade)}")
        remaining.removeIf { !checkWord(guess, it, grade) }
        println("There are ${remaining.size} potential words left")
        guess = bestGuess(remaining)
    }

    println("Word is $guess, took $tries tries")
}

fun testAnswer() {
    var answer = ""
    while (answer.length != 5 || !allWords.contains(answer)) {
        print("Enter answer > ")
        answer = readLine() ?: ""
        if (answer.length != 5 || !allWords.contains(answer)) {
            println("Please enter a valid 5 letter word")
        }
    }

    runTest(answer)
}

fun bestFirstGuess() {
    val rankedWords = allWords.associateWith { guess ->
        var totalRemaining = 0L
        for (answer in allWords) {
            val grade = gradeWord(guess, answer)
            totalRemaining += allWords.filter { checkWord(guess, it, grade) }.size
        }

        println("$guess scored a ${totalRemaining.toFloat() / allWords.size}")
        File("results.txt").appendText("$guess scored a ${totalRemaining.toFloat() / allWords.size}\n")

        totalRemaining
    }

    println(); println(); println(); println(); println()
    println("RESULTS - ")

    val sorted = rankedWords.toList().sortedBy { it.second }
    for (result in sorted) {
        println("${result.first} - ${result.second.toFloat() / allWords.size}")
        File("sorted.txt").appendText("${result.first} - ${result.second.toFloat() / allWords.size}\n")
    }
}

fun buildCheatSheet() {
    val first = getValidWord()

    for (l1 in 0..2) {
        for (l2 in 0..2) {
            for (l3 in 0..2) {
                for (l4 in 0..2) {
                    for (l5 in 0..2) {
                        val grade = (l1 shl 8) or (l2 shl 6) or (l3 shl 4) or (l4 shl 2) or l5
                        val remaining = allWords.filter { checkWord(first, it, grade) }
                        if (remaining.isNotEmpty()) {
                            val best = bestGuess(remaining = remaining.toSet())
                            println("${prettyGrade(grade)} - $best")
                            File("$first.txt").appendText("${prettyGrade(grade)} - $best\n")
                        }
                    }
                }
            }
        }
    }
}

fun bestGuess(remaining: Set<String>): String {
    var best = ""
    var bestTotal = Long.MAX_VALUE

    for (guess in remaining) {
        var totalRemaining = 0L
        for (answer in remaining) {
            val grade = gradeWord(guess, answer)
            totalRemaining += remaining.filter { checkWord(guess, it, grade) }.size
            if (totalRemaining > bestTotal)
                break
        }

        if (totalRemaining < bestTotal) {
            bestTotal = totalRemaining
            best = guess
        }
    }

    return best
}

fun prettyGrade(grade: Int): String {
    var retval = ""
    for (i in 8 downTo 0 step 2) {
        retval += when ((grade shr i) and 3) {
            0 -> "C"
            1 -> "w"
            2 -> "."
            else -> throw RuntimeException("Invalid grade sent in")
        }
    }

    return retval
}

fun stringToGrade(input: String): Int {
    if (input.length != 5)
        throw RuntimeException("Invalid grade input")

    var grade = 0

    for (i in input) {
        grade = grade shl 2
        grade += when (i) {
            'C' -> 0
            'w' -> 1
            '.' -> 2
            else -> throw RuntimeException("Invalid grade input")
        }
    }

    return grade
}

fun loadWords() {
    val uncommon = mutableSetOf<String>()
    val common = mutableSetOf<String>()

    File("assets/wordlists/uncommon.txt").forEachLine {
        uncommon.add(it)
    }

    File("assets/wordlists/common.txt").forEachLine {
        common.add(it)
    }

    commonWords = common.toSet()
    allWords = uncommon union common
}

// TODO - This doesn't do repeat letters correctly. Still works, just not optimal
fun gradeWord(guess: String, answer: String) : Int  {
    // Return 5 sets of 2 bits
    //  00 - Correct
    //  01 - Wrong spot
    //  10 - Not in string
    var total = 0
    for (i in 0 until 5) {
        total = total shl 2
        val thisSpot =
            when {
                guess[i] == answer[i] -> 0
                answer.contains(guess[i]) -> 1
                else -> 2
            }
        total += thisSpot
    }

    return total
}

fun checkWord(guess: String, answer: String, grade: Int) : Boolean {
    return gradeWord(guess, answer) == grade
}

fun getValidWord(remaining: Set<String> = allWords): String {
    var word = ""
    while (word.length != 5 || !remaining.contains(word)) {
        print("Enter word > ")
        word = readLine() ?: ""
        if (word.length != 5 || !allWords.contains(word)) {
            println("Please enter a valid 5 letter word")
        }
    }

    return word
}

fun getSecondGuesses(first: String): Map<Int, String> {
    val seconds = mutableMapOf<Int, String>()

    val lines = File("$first.txt").readLines()
    for (line in lines) {
        val grade = stringToGrade(line.substring(0, 5))
        seconds[grade] = line.substring(8, 13)
    }

    return seconds
}

fun testAllWords() {
    val first = getValidWord()
    val seconds = getSecondGuesses(first)

    val totalTries = IntArray(20) { 0 }

    for (answer in allWords) {
        println("Testing for the answer $answer")

        val remaining = allWords.toMutableSet()

        // First guess
        var tries = 1
        var guess = first
        if (answer != guess) {
            val grade = gradeWord(guess, answer)
            remaining.removeIf { !checkWord(guess, it, grade) }
            guess = seconds[grade] ?: throw Exception("Second word not found for ${prettyGrade(grade)}")
            tries++
        }

        while (guess != answer && remaining.size > 0) {
            val grade = gradeWord(guess, answer)
            remaining.removeIf { !checkWord(guess, it, grade) }
            guess = bestGuess(remaining)
            tries++
        }

        totalTries[tries]++
        println("Word is $guess, took $tries tries")
    }

    for (i in 0 until 20) {
        println("Number of words in $i tries: ${totalTries[i]}, ${totalTries[i].toFloat() * 100.0f / allWords.size.toFloat()}%")
    }
}