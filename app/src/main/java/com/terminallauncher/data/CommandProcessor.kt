package com.terminallauncher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import java.io.File

sealed class TerminalOutput {
    data class TextLine(val text: String, val dim: Boolean = false) : TerminalOutput()
    data class SelectableItem(val label: String, val id: String) : TerminalOutput()
    data class Error(val text: String) : TerminalOutput()
}

sealed class CommandAction {
    object None : CommandAction()
    data class InstallApp(val packageName: String) : CommandAction()
}

data class CommandResult(
    val output: List<TerminalOutput>,
    val action: CommandAction = CommandAction.None
)

class CommandProcessor(private val context: Context) {

    private var currentDir: File = Environment.getExternalStorageDirectory()

    val currentPath: String get() = currentDir.absolutePath

    private val commands = setOf("cd", "mkdir", "ls", "list", "install", "pwd", "rm", "touch", "cat", "shortcut", "help", "info", "uninstall", "clear")

    fun isCommand(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.startsWith(",")) return true // Google search
        val first = trimmed.split(" ").firstOrNull()?.lowercase() ?: return false
        return first in commands
    }

    // Will be set by ViewModel to handle shortcut changes
    var onShortcutChange: ((direction: String, query: String) -> Unit)? = null

    fun execute(input: String): CommandResult {
        val trimmed = input.trim()

        // Google search: , query
        if (trimmed.startsWith(",")) {
            val query = trimmed.removePrefix(",").trim()
            return googleSearch(query)
        }

        val parts = trimmed.split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val arg = parts.getOrNull(1)?.trim() ?: ""

        return when (cmd) {
            "cd" -> cd(arg)
            "mkdir" -> mkdir(arg)
            "ls", "list" -> ls()
            "pwd" -> CommandResult(listOf(TerminalOutput.TextLine(currentDir.absolutePath)))
            "rm" -> rm(arg)
            "touch" -> touch(arg)
            "cat" -> cat(arg)
            "install" -> searchPlayStore(arg)
            "shortcut" -> shortcut(arg)
            "info" -> appInfo(arg)
            "uninstall" -> uninstall(arg)
            "clear" -> CommandResult(emptyList()) // handled by ViewModel
            "help" -> help()
            else -> CommandResult(listOf(TerminalOutput.Error("unknown command: $cmd")))
        }
    }

    private fun help(): CommandResult = CommandResult(listOf(
        TerminalOutput.TextLine("commands:", dim = true),
        TerminalOutput.TextLine("  ls              list files"),
        TerminalOutput.TextLine("  cd <dir>        change directory"),
        TerminalOutput.TextLine("  mkdir <name>    create folder"),
        TerminalOutput.TextLine("  rm <name>       delete file/folder"),
        TerminalOutput.TextLine("  touch <name>    create file"),
        TerminalOutput.TextLine("  cat <file>      view file"),
        TerminalOutput.TextLine("  pwd             current path"),
        TerminalOutput.TextLine("  install <app>   search play store"),
        TerminalOutput.TextLine("  shortcut left   set left swipe app"),
        TerminalOutput.TextLine("  shortcut right  set right swipe app"),
        TerminalOutput.TextLine("  info <app>      app settings page"),
        TerminalOutput.TextLine("  uninstall <app> uninstall app"),
        TerminalOutput.TextLine("  clear           clear terminal"),
        TerminalOutput.TextLine("  , <query>       google search"),
        TerminalOutput.TextLine("  help            show this"),
    ))

    // App info resolver — set by ViewModel
    var onAppInfo: ((query: String) -> Unit)? = null

    // Uninstall resolver — set by ViewModel
    var onUninstall: ((query: String) -> Unit)? = null

    private fun appInfo(query: String): CommandResult {
        if (query.isEmpty()) return CommandResult(listOf(TerminalOutput.Error("info: specify an app name")))
        onAppInfo?.invoke(query)
        return CommandResult(listOf(TerminalOutput.TextLine("opening settings for '$query'...", dim = true)))
    }

    private fun uninstall(query: String): CommandResult {
        if (query.isEmpty()) return CommandResult(listOf(TerminalOutput.Error("uninstall: specify an app name")))
        onUninstall?.invoke(query)
        return CommandResult(listOf(TerminalOutput.TextLine("uninstalling '$query'...", dim = true)))
    }

    fun tabComplete(input: String): String? {
        val trimmed = input.trim().lowercase()
        if (trimmed.isEmpty()) return null

        // Complete command names
        if (!trimmed.contains(" ")) {
            val match = commands.filter { it.startsWith(trimmed) }
            if (match.size == 1) return match.first() + " "
            return null
        }

        // Complete file/folder names for file commands
        val parts = trimmed.split(" ", limit = 2)
        val cmd = parts[0]
        val partial = parts.getOrNull(1) ?: ""

        if (cmd in setOf("cd", "cat", "rm") && partial.isNotEmpty()) {
            val files = currentDir.listFiles() ?: return null
            val matches = files.filter { it.name.lowercase().startsWith(partial.lowercase()) }
            if (matches.size == 1) {
                val name = matches.first().name
                val suffix = if (matches.first().isDirectory) "/" else ""
                return "$cmd $name$suffix"
            }
        }

        return null
    }

    private fun shortcut(arg: String): CommandResult {
        val parts = arg.split(" ", limit = 2)
        val direction = parts[0].lowercase()

        if (direction != "left" && direction != "right") {
            return CommandResult(listOf(
                TerminalOutput.TextLine("usage: shortcut left/right <app name>"),
                TerminalOutput.TextLine("  shortcut left whatsapp"),
                TerminalOutput.TextLine("  shortcut right chrome"),
            ))
        }

        val appQuery = parts.getOrNull(1)?.trim() ?: ""
        if (appQuery.isEmpty()) {
            return CommandResult(listOf(TerminalOutput.Error("shortcut $direction: specify an app name")))
        }

        onShortcutChange?.invoke(direction, appQuery)
        return CommandResult(listOf(TerminalOutput.TextLine("searching for '$appQuery'...", dim = true)))
    }

    private fun googleSearch(query: String): CommandResult {
        if (query.isEmpty()) return CommandResult(listOf(TerminalOutput.Error("search what?")))
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            CommandResult(listOf(TerminalOutput.TextLine("googling: $query", dim = true)))
        } catch (_: Exception) {
            CommandResult(listOf(TerminalOutput.Error("can't open browser")))
        }
    }

    private fun cd(path: String): CommandResult {
        if (path.isEmpty()) {
            currentDir = Environment.getExternalStorageDirectory()
            return CommandResult(listOf(TerminalOutput.TextLine(currentDir.absolutePath, dim = true)))
        }

        val target = if (path == "..") {
            currentDir.parentFile ?: currentDir
        } else if (path.startsWith("/")) {
            File(path)
        } else {
            File(currentDir, path)
        }

        return if (target.exists() && target.isDirectory) {
            currentDir = target
            CommandResult(listOf(TerminalOutput.TextLine(currentDir.absolutePath, dim = true)))
        } else {
            CommandResult(listOf(TerminalOutput.Error("cd: no such directory: $path")))
        }
    }

    private fun mkdir(name: String): CommandResult {
        if (name.isEmpty()) return CommandResult(listOf(TerminalOutput.Error("mkdir: missing name")))

        val dir = File(currentDir, name)
        return if (dir.mkdirs()) {
            CommandResult(listOf(TerminalOutput.TextLine("created: $name", dim = true)))
        } else {
            CommandResult(listOf(TerminalOutput.Error("mkdir: failed to create $name")))
        }
    }

    private fun ls(): CommandResult {
        val files = currentDir.listFiles()
        if (files == null || files.isEmpty()) {
            return CommandResult(listOf(TerminalOutput.TextLine("(empty)", dim = true)))
        }

        val sorted = files.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
        val lines = sorted.map { file ->
            val prefix = if (file.isDirectory) "📁 " else "   "
            val suffix = if (file.isDirectory) "/" else ""
            TerminalOutput.TextLine("$prefix${file.name}$suffix")
        }

        return CommandResult(
            listOf(TerminalOutput.TextLine("${currentDir.absolutePath}:", dim = true)) + lines
        )
    }

    private fun rm(name: String): CommandResult {
        if (name.isEmpty()) return CommandResult(listOf(TerminalOutput.Error("rm: missing name")))

        val file = File(currentDir, name)
        return if (!file.exists()) {
            CommandResult(listOf(TerminalOutput.Error("rm: not found: $name")))
        } else if (file.deleteRecursively()) {
            CommandResult(listOf(TerminalOutput.TextLine("deleted: $name", dim = true)))
        } else {
            CommandResult(listOf(TerminalOutput.Error("rm: failed to delete $name")))
        }
    }

    private fun touch(name: String): CommandResult {
        if (name.isEmpty()) return CommandResult(listOf(TerminalOutput.Error("touch: missing name")))

        val file = File(currentDir, name)
        return try {
            file.createNewFile()
            CommandResult(listOf(TerminalOutput.TextLine("created: $name", dim = true)))
        } catch (e: Exception) {
            CommandResult(listOf(TerminalOutput.Error("touch: ${e.message}")))
        }
    }

    private fun cat(name: String): CommandResult {
        if (name.isEmpty()) return CommandResult(listOf(TerminalOutput.Error("cat: missing name")))

        val file = File(currentDir, name)
        if (!file.exists()) return CommandResult(listOf(TerminalOutput.Error("cat: not found: $name")))
        if (file.isDirectory) return CommandResult(listOf(TerminalOutput.Error("cat: is a directory")))

        return try {
            val lines = file.readLines().take(50).map { TerminalOutput.TextLine(it) }
            if (lines.isEmpty()) {
                CommandResult(listOf(TerminalOutput.TextLine("(empty file)", dim = true)))
            } else {
                CommandResult(lines)
            }
        } catch (e: Exception) {
            CommandResult(listOf(TerminalOutput.Error("cat: ${e.message}")))
        }
    }

    private fun searchPlayStore(query: String): CommandResult {
        if (query.isEmpty()) return CommandResult(listOf(TerminalOutput.Error("install: what app?")))

        // Open Play Store search
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            CommandResult(listOf(TerminalOutput.TextLine("opening play store: $query", dim = true)))
        } catch (e: Exception) {
            // Fallback to web
            try {
                val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=$query")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                CommandResult(listOf(TerminalOutput.TextLine("opening play store: $query", dim = true)))
            } catch (_: Exception) {
                CommandResult(listOf(TerminalOutput.Error("install: can't open play store")))
            }
        }
    }
}
