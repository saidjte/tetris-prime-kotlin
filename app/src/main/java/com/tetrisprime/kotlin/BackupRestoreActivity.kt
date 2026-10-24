package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Phase 10.3 — backup & restore UI.
 *
 * Four actions:
 *  1. Export — write all data to tetris-prime-v4-backup.json
 *  2. Import — restore from the backup file (with .bak safety rotation)
 *  3. Factory Reset — clear everything (with confirmation)
 *  4. Storage Info — show per-namespace byte sizes
 */
class BackupRestoreActivity : AppCompatActivity() {

    private lateinit var statusLabel: TextView
    private lateinit var storageInfoContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TranslationManager.init(this)

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 60, 32, 80)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        // Header
        container.addView(TextView(this).apply {
            text = "💾 النسخ الاحتياطي والاستعادة"
            textSize = 22f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        })

        // Schema info
        val schemaInfo = BackupManager.backupInfo(this)
        val schemaText = if (schemaInfo != null) {
            "النسخة الحالية: v${schemaInfo.appVersion}\n" +
            "تاريخ النسخ: ${BackupManager.formatTimestamp(schemaInfo.createdAt)}\n" +
            "الحجم: ${schemaInfo.byteSize} بايت\n" +
            "المساحات: ${schemaInfo.namespaceCount}\n" +
            "إصدار المخطط: v${schemaInfo.schemaVersion}"
        } else {
            "لا يوجد نسخة احتياطية بعد\n" +
            "إصدار المخطط: v${StorageSchemas.CURRENT_VERSION}"
        }
        container.addView(TextView(this).apply {
            text = schemaText
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        // Export button
        container.addView(actionButton("📤 تصدير نسخة احتياطية", 0xFF8B5CF6.toInt()) {
            val result = BackupManager.export(this)
            when (result) {
                is BackupManager.ExportResult.Success -> {
                    statusLabel.text = "✅ تم التصدير: ${result.byteSize} بايت، ${result.namespaceCount} مساحة"
                    statusLabel.setTextColor(0xFF34D399.toInt())
                    refreshStorageInfo()
                    Toast.makeText(this, "تم التصدير بنجاح", Toast.LENGTH_SHORT).show()
                }
                is BackupManager.ExportResult.Failure -> {
                    statusLabel.text = "❌ فشل التصدير: ${result.reason}"
                    statusLabel.setTextColor(0xFFEF4444.toInt())
                }
            }
        })

        // Import button
        container.addView(actionButton("📥 استيراد نسخة احتياطية", 0xFF22D3EE.toInt()) {
            val result = BackupManager.importDefault(this)
            when (result) {
                is BackupManager.ImportResult.Success -> {
                    statusLabel.text = "✅ تم الاستيراد: ${result.namespaceCount} مساحة (مخطط v${result.schemaVersion})"
                    statusLabel.setTextColor(0xFF34D399.toInt())
                    refreshStorageInfo()
                    Toast.makeText(this, "تم الاستيراد بنجاح", Toast.LENGTH_SHORT).show()
                }
                is BackupManager.ImportResult.Failure -> {
                    statusLabel.text = "❌ فشل الاستيراد: ${result.reason}"
                    statusLabel.setTextColor(0xFFEF4444.toInt())
                }
            }
        })

        // Factory reset button (with confirmation)
        container.addView(actionButton("🗑️ إعادة ضبط المصنع", 0xFFEF4444.toInt()) {
            showFactoryResetConfirmation()
        })

        // Status label
        statusLabel = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
            minLines = 2
        }
        container.addView(statusLabel)

        // Storage info header
        container.addView(TextView(this).apply {
            text = "📊 تفاصيل التخزين"
            textSize = 16f
            setTextColor(0xFFA78BFA.toInt())
            setPadding(0, 16, 0, 8)
        })

        // Storage info container
        storageInfoContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF1E1B4B.toInt())
            d.cornerRadius = 14f
            background = d
        }
        container.addView(storageInfoContainer)

        // Done button
        container.addView(actionButton("تم", 0xFF1E1B4B.toInt()) {
            finish()
        })

        scroll.addView(container)
        setContentView(scroll)

        refreshStorageInfo()
    }

    private fun actionButton(text: String, bgColor: Int, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setBackgroundColor(bgColor)
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }
    }

    private fun refreshStorageInfo() {
        storageInfoContainer.removeAllViews()
        val infos = StorageManager.allNamespaceInfo(this)
        var totalBytes = 0
        for (info in infos) {
            totalBytes += info.byteSize
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
            }
            val name = TextView(this).apply {
                text = info.name
                textSize = 11f
                setTextColor(0xFFCBD5E1.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val size = TextView(this).apply {
                text = "${info.keyCount} مفتاح · ${info.byteSize}B"
                textSize = 11f
                setTextColor(0xFF94A3B8.toInt())
                gravity = Gravity.END
            }
            row.addView(name)
            row.addView(size)
            storageInfoContainer.addView(row)
        }
        // Total
        val totalRow = TextView(this).apply {
            text = "الإجمالي: $totalBytes بايت"
            textSize = 12f
            setTextColor(0xFFFBBF24.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 12, 0, 0)
            gravity = Gravity.CENTER
        }
        storageInfoContainer.addView(totalRow)
    }

    private fun showFactoryResetConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ تأكيد إعادة الضبط")
            .setMessage("سيتم حذف جميع البيانات: الإعدادات، الإحصائيات، الإنجازات، " +
                       "المهمات، المغامرة، المتجر، لوحة الصدارة، والنسخ الاحتياطية.\n\n" +
                       "هذا الإجراء لا يمكن التراجع عنه. هل أنت متأكد؟")
            .setPositiveButton("نعم، احذف كل شيء") { _, _ ->
                val cleared = BackupManager.factoryReset(this)
                statusLabel.text = "🗑️ تم حذف $cleared مساحة"
                statusLabel.setTextColor(0xFFEF4444.toInt())
                refreshStorageInfo()
                Toast.makeText(this, "تمت إعادة الضبط", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
