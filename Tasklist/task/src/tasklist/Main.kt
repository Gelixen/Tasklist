package tasklist

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalTime
import java.util.*
import kotlin.system.exitProcess

val tasks: MutableList<Task> = mutableListOf()

val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

val type = Types.newParameterizedType(List::class.java, Task::class.java)
val taskListAdapter = moshi.adapter<List<Task?>>(type)

data class Task(
    val date: String,
    val time: String,
    var priority: String,
    val dueTag: String,
    val taskLines: List<String>
)

enum class Action(val inputName: String) {
    ADD("add"),
    PRINT("print"),
    EDIT("edit"),
    DELETE("delete"),
    END("end")
}

fun main() {
    loadInitialDataFromFile()
    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        when (readln().uppercase().trim()) {
            Action.ADD.toString() -> addTask()
            Action.PRINT.toString() -> printTasks()
            Action.EDIT.toString() -> editTask()
            Action.DELETE.toString() -> deleteTask()
            Action.END.toString() -> end()
            else -> println("The input action is invalid")
        }
    }
}

fun loadInitialDataFromFile() {
    val file = File("tasklist.json")
    try {
        val json = file.readText()
        val elements: List<Task?>? = taskListAdapter.fromJson(json)
        tasks.addAll(elements as List<Task>)
    } catch (ex: FileNotFoundException) {
        return
    }
}

fun editTask() {
    printTasks()

    if (tasks.isNotEmpty()) {
        val taskIndex = requestTaskIndex()
        val oldTask = tasks[taskIndex]
        var updatedTask: Optional<Task>

        do {
            println("Input a field to edit (priority, date, time, task):")

            updatedTask = when (readln()) {
                "priority" -> Optional.of(oldTask.copy(priority = parsePriority()))
                "date" -> Optional.of(oldTask.copy(date = parseDate()))
                "time" -> Optional.of(oldTask.copy(time = parseTime()))
                "task" -> Optional.of(oldTask.copy(taskLines = parseTaskLines()))
                else -> {
                    println("Invalid field")
                    Optional.empty<Task>()
                }
            }

        } while (updatedTask.isEmpty)

        updatedTask.ifPresent { tasks[taskIndex] = it }
        println("The task is changed")
    }
}

fun deleteTask() {
    printTasks()

    if (tasks.isNotEmpty()) {
        val taskIndex = requestTaskIndex()

        tasks.removeAt(taskIndex)
        println("The task is deleted")
    }
}

private fun requestTaskIndex(): Int {
    var taskIndex = -1
    var isValidNumber = false
    do {
        println("Input the task number (1-${tasks.size}):")
        try {
            val taskNumber = readln().toInt()
            taskIndex = taskNumber - 1

            if (taskIndex in tasks.indices) {
                isValidNumber = true
            } else {
                println("Invalid task number")
            }

        } catch (ex: Exception) {
            println("Invalid task number")
        }
    } while (!isValidNumber)
    return taskIndex
}

private fun end() {
    val json = taskListAdapter.toJson(tasks)

    writeToFile(json)

    println("Tasklist exiting!")
    exitProcess(1)
}

private fun writeToFile(text: String) {
    val file = File("tasklist.json")
    file.writeText(text)
}

fun printTasks() {
    if (tasks.isEmpty()) {
        println("No tasks have been input")
    } else {
        println("+----+------------+-------+---+---+--------------------------------------------+")
        println("| N  |    Date    | Time  | P | D |                   Task                     |")
        println("+----+------------+-------+---+---+--------------------------------------------+")
        for (i in tasks.indices) {
            val (date, time, priority, dueTag, taskLines) = tasks[i]
            printNumber(i + 1)
            printCell(date)
            printCell(time)
            printPriority(priority)
            printDueTag(dueTag)
            printTaskLines(taskLines)
            println("+----+------------+-------+---+---+--------------------------------------------+")
        }
    }
}

fun printTaskLines(taskLines: List<String>) {
    taskLines.flatMap { it.chunked(44) }
        .map { it.padEnd(44, ' ') }
        .map { "|$it|" }
        .joinToString(separator = "\n|    |            |       |   |   ")
        .forEach(::print)

    println()
}

fun printDueTag(dueTag: String) {
    val colorCode = when (dueTag) {
        "I" -> "102"
        "T" -> "103"
        "O" -> "101"
        else -> ""
    }
    printCell("\u001B[${colorCode}m \u001B[0m")
}

fun printPriority(priority: String) {
    val colorCode = when (priority) {
        "C" -> "101"
        "H" -> "103"
        "N" -> "102"
        "L" -> "104"
        else -> ""
    }
    printCell("\u001B[${colorCode}m \u001B[0m")
}

fun printNumber(number: Int) {
    val paddedNumber = number.toString().padEnd(2, ' ')
    printCell(paddedNumber)
}

private fun printCell(text: String) {
    print("| $text ")
}

fun addTask() {
    val priority = parsePriority()
    val date = parseDate()
    val time = parseTime()
    val dueTag = calculateDueTag(date)
    val taskLines = parseTaskLines()

    if (taskLines.isNotEmpty()) {
        tasks.add(Task(date, time, priority, dueTag, taskLines))
    }
}

private fun parseTaskLines(): List<String> {
    println("Input a new task (enter a blank line to end):")
    var taskLineInput = readln().trim()

    if (taskLineInput.isBlank()) {
        println("The task is blank")
        return emptyList()
    }

    val taskLines = mutableListOf<String>()

    while (taskLineInput.isNotEmpty()) {
        taskLines.add(taskLineInput)
        taskLineInput = readln().trim()
    }
    return taskLines
}

private fun calculateDueTag(date: String): String {
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    val dueDate = LocalDate.parse(date)
    val daysUntil = currentDate.daysUntil(dueDate)

    return when {
        daysUntil == 0 -> "T"
        daysUntil > 0 -> "I"
        else -> "O"
    }
}

private fun parseTime(): String {
    var isoTime = ""
    var isValidTime = false

    do {
        println("Input the time (hh:mm):")
        val timeInput = readln()

        try {
            val (hour, minute) = timeInput.split(':')
            isoTime = "${padZero(hour)}:${padZero(minute)}"
            LocalTime.parse(isoTime)
            isValidTime = true
        } catch (ex: Exception) {
            println("The input time is invalid")
        }

    } while (!isValidTime)
    return isoTime
}

private fun parseDate(): String {
    var isoDate = ""
    var isValidDate = false

    do {
        println("Input the date (yyyy-mm-dd):")
        val dateInput = readln()

        try {
            val (year, month, day) = dateInput.split('-')
            isoDate = "$year-${padZero(month)}-${padZero(day)}"
            LocalDate.parse(isoDate)
            isValidDate = true
        } catch (ex: Exception) {
            println("The input date is invalid")
        }

    } while (!isValidDate)
    return isoDate
}

private fun parsePriority(): String {
    var priorityInput: String
    val priorityOptions = arrayOf("C", "H", "N", "L")

    do {
        println("Input the task priority (C, H, N, L):")
        priorityInput = readln().uppercase()
    } while (!priorityOptions.contains(priorityInput))

    return priorityInput
}

private fun padZero(string: String) = string.padStart(2, '0')




