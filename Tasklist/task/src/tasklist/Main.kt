package tasklist

import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

fun main() {
    val manager = TaskManager(import())
    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        when (readln().trim()) {
            "add" -> manager.addTask()
            "print" -> manager.printTasks()
            "delete" -> manager.deleteTask()
            "edit" -> manager.editTask()
            "end" -> break
            else -> println("The input action is invalid")
        }

    }
    export(manager.tasks)
    println("Tasklist exiting!")
}


fun export(tasks: MutableList<Task>) {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val tasksType = Types.newParameterizedType(MutableList::class.java, Task::class.java)
    val tasksAdapter = moshi.adapter<MutableList<Task>>(tasksType)
    try{
        val jsonFile = File("tasklist.json")
        jsonFile.writeText(tasksAdapter.toJson(tasks))
    }catch(ex:Exception){

    }
}

fun import(): MutableList<Task> {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val tasksType = Types.newParameterizedType(MutableList::class.java, Task::class.java)
    val tasksAdapter = moshi.adapter<MutableList<Task?>>(tasksType)
    return try {
        val jsonFile = File("tasklist.json")
        if (jsonFile.exists()) {
            val tmp = tasksAdapter.fromJson(jsonFile.readText())
            val tmp2 = tmp ?: mutableListOf<Task?>()
            tmp2.filterNotNull().toMutableList()
        } else mutableListOf<Task>()
    } catch (ex: Exception) {
        mutableListOf<Task>()
    }
}

class Task(var lines: List<String>, var date: String, var time: String, var priority: Char) {
    fun dueTag(): Char {
        val taskDate = LocalDate.parse(date)
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
        val numberOfDays = currentDate.daysUntil(taskDate)
        return if (numberOfDays > 0) {
            'I'
        } else if (numberOfDays < 0) {
            'O'
        } else {
            'T'
        }
    }

    fun colorPriority(): String {
        return when (priority) {
            'C' -> "\u001B[101m \u001B[0m"
            'H' -> "\u001B[103m \u001B[0m"
            'N' -> "\u001B[102m \u001B[0m"
            'L' -> "\u001B[104m \u001B[0m"
            else -> ""
        }
    }

    fun colorTag(): String {
        return when (dueTag()) {
            'I' -> "\u001B[102m \u001B[0m"
            'T' -> "\u001B[103m \u001B[0m"
            'O' -> "\u001B[101m \u001B[0m"
            else -> ""
        }
    }

    override fun toString(): String {
        var str = "| $date | $time | ${colorPriority()} | ${colorTag()} |                                            |"
        for (line in lines) {
            line.chunked(44)
        }
        return super.toString()
    }
}

class TaskManager(val tasks: MutableList<Task> = mutableListOf()) {
    //private val tasks = mutableListOf<Task>()

    private fun getPriority(): Char {
        while (true) {
            println("Input the task priority (C, H, N, L):")
            val priorityStr = readln()
            if ("[cChHnNlL]".toRegex().matches(priorityStr)) {
                return priorityStr.uppercase().first()
            }
        }

    }

    private fun getDate(): String {
        while (true) {
            println("Input the date (yyyy-mm-dd):")
            val dateStr = readln()
            try {
                val tmp = dateStr.split("-").map { it.toInt() }
                if (tmp.size != 3) throw Exception()
                val date = LocalDate(tmp[0], tmp[1], tmp[2])
                return date.toString()
            } catch (ex: Exception) {
                println("The input date is invalid")
            }
        }
    }

    private fun getTime(): String {
        while (true) {
            println("Input the time (hh:mm):")
            val timeStr = readln()
            try {
                val tmp = timeStr.split(":").map { it.toInt() }
                if (tmp.size != 2) throw Exception()
                val dateTimeCurrent = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0"))
                val dateCurrent = dateTimeCurrent.date
                val time =
                    LocalDateTime(dateCurrent.year, dateCurrent.monthNumber, dateCurrent.dayOfMonth, tmp[0], tmp[1])
                return time.toString().split("T")[1]
            } catch (ex: Exception) {
                println("The input time is invalid")
            }
        }
    }

    private fun getLines(): List<String> {
        val resTask = mutableListOf<String>()
        println("Input a new task (enter a blank line to end):")
        while (true) {
            val input = readln().trim()
            if (input == "") break
            resTask.add(input)
        }
        if (resTask.isEmpty()) throw Exception("The task is blank")
        return resTask.toList()
    }

    fun addTask() {
        try {
            val priority = getPriority()
            val date = getDate()
            val time = getTime()
            val lines = getLines()
            tasks.add(Task(lines, date, time, priority))
        } catch (ex: Exception) {
            println(ex.message)
        }
    }

    private fun checkLineLength(str: String): String {
        if (str.length == 44) return str
        var tmp = str
        while (tmp.length < 44) {
            tmp += " "
        }
        return tmp
    }

    fun printTasks() {
        if (tasks.isEmpty()) {
            println("No tasks have been input")
            return
        }
        tableLine()
        headerLine()
        tableLine()
        for (taskId in tasks.indices) {
            print(if (taskId < 9) "| ${taskId + 1}  " else "| ${taskId + 1} ")
            print("| ${tasks[taskId].date} | ${tasks[taskId].time} | ${tasks[taskId].colorPriority()} | ${tasks[taskId].colorTag()} |")
            for (taskLineIndex in tasks[taskId].lines.indices) {
                val line = tasks[taskId].lines[taskLineIndex].chunked(44)
                for (chunkIndex in line.indices) {
                    if (taskLineIndex == 0 && chunkIndex == 0) {
                        print(checkLineLength(line[chunkIndex]) + "|")
                    } else {
                        println("|    |            |       |   |   |" + checkLineLength(line[chunkIndex]) + "|")
                    }
                }
            }
            tableLine()
        }
    }

    private fun getTaskNumber(): Int {
        while (true) {
            println("Input the task number (1-${tasks.size}):")
            try {
                val num = readln().toInt()
                if (num !in 1..tasks.size) throw Exception("Invalid task number")
                return num - 1
            } catch (ex: Exception) {
                println("Invalid task number")
            }
        }
    }

    fun deleteTask() {
        printTasks()
        if (tasks.isEmpty()) return
        tasks.removeAt(getTaskNumber())
        println("The task is deleted")
    }

    fun editTask() {
        printTasks()
        if (tasks.isEmpty()) return
        val task = tasks[getTaskNumber()]
        while (true) {
            println("Input a field to edit (priority, date, time, task):")
            when (readln()) {
                "priority" -> {
                    task.priority = getPriority()
                    break
                }

                "date" -> {
                    task.date = getDate()
                    break
                }

                "time" -> {
                    task.time = getTime()
                    break
                }

                "task" -> {
                    task.lines = getLines()
                    break
                }

                else -> println("Invalid field")
            }
        }
        println("The task is changed")
    }

    private fun tableLine() {
        println("+----+------------+-------+---+---+--------------------------------------------+")
    }

    private fun headerLine() {
        println("| N  |    Date    | Time  | P | D |                   Task                     |")
    }
}



