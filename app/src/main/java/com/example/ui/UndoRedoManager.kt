package com.example.ui

class UndoRedoManager(private val maxHistory: Int = 50) {
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    
    fun push(content: String) {
        if (undoStack.isNotEmpty() && undoStack.last() == content) {
            return
        }
        undoStack.addLast(content)
        if (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }
    
    fun undo(current: String): String? {
        if (!canUndo()) return null
        
        if (undoStack.isNotEmpty() && undoStack.last() == current) {
            val popped = undoStack.removeLast()
            redoStack.addLast(popped)
        } else {
            redoStack.addLast(current)
        }
        
        return if (undoStack.isNotEmpty()) undoStack.last() else null
    }
    
    fun redo(current: String): String? {
        if (!canRedo()) return null
        
        val popped = redoStack.removeLast()
        undoStack.addLast(current)
        return popped
    }
    
    fun canUndo(): Boolean {
        return undoStack.isNotEmpty() && (undoStack.size > 1 || undoStack.last() != "")
    }
    
    fun canRedo(): Boolean {
        return redoStack.isNotEmpty()
    }
    
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
