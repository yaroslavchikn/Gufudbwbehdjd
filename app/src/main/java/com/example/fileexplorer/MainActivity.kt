package com.example.fileexplorer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var currentPath: TextView
    private lateinit var backButton: Button
    private var currentDir: File = Environment.getExternalStorageDirectory()
    private val fileList = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        currentPath = findViewById(R.id.currentPath)
        backButton = findViewById(R.id.backButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        checkPermissions()
        
        backButton.setOnClickListener {
            val parent = currentDir.parentFile
            if (parent != null) {
                currentDir = parent
                loadFiles()
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ использует автоматические разрешения
            loadFiles()
        } else {
            val permissions = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            
            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
            } else {
                loadFiles()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadFiles()
            } else {
                Toast.makeText(this, "Разрешения необходимы для работы", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFiles() {
        currentPath.text = "📁 ${currentDir.absolutePath}"
        fileList.clear()
        
        try {
            val files = currentDir.listFiles()
            if (files != null) {
                fileList.addAll(files.filter { it.canRead() })
                fileList.sortWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Нет доступа к папке", Toast.LENGTH_SHORT).show()
        }
        
        recyclerView.adapter = FileAdapter(fileList) { file ->
            if (file.isDirectory && file.canRead()) {
                currentDir = file
                loadFiles()
            } else if (file.isFile) {
                Toast.makeText(this, "📄 ${file.name}\nРазмер: ${formatSize(file.length())}", 
                    Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Нет доступа", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class FileAdapter(
        private val files: List<File>,
        private val onItemClick: (File) -> Unit
    ) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FileViewHolder {
            val view = layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false)
            return FileViewHolder(view)
        }

        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            val file = files[position]
            holder.text1.text = if (file.isDirectory) "📁 " else "📄 "
            holder.text1.append(file.name)
            
            val size = if (file.isDirectory) "📂 Папка" else formatSize(file.length())
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(Date(file.lastModified()))
            holder.text2.text = "$size  |  $date"
            
            holder.itemView.setOnClickListener { onItemClick(file) }
        }

        override fun getItemCount() = files.size

        inner class FileViewHolder(itemView: android.view.View) : 
            RecyclerView.ViewHolder(itemView) {
            val text1: TextView = itemView.findViewById(android.R.id.text1)
            val text2: TextView = itemView.findViewById(android.R.id.text2)
        }
    }

    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}
