package me.blog.korn123.easydiary.extensions

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.preference.PreferenceManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.*
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import com.google.gson.GsonBuilder
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.formatMinutesToTimeString
import com.simplemobiletools.commons.extensions.isBlackAndWhiteTheme
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.views.*
import io.github.aafactory.commons.extensions.baseConfig
import io.github.aafactory.commons.utils.CommonUtils
import io.github.aafactory.commons.utils.DateUtils
import io.github.aafactory.commons.views.ModalView
import kotlinx.android.synthetic.main.dialog_message.view.*
import me.blog.korn123.commons.utils.EasyDiaryUtils
import me.blog.korn123.commons.utils.FontUtils
import me.blog.korn123.easydiary.R
import me.blog.korn123.easydiary.activities.DiaryInsertActivity
import me.blog.korn123.easydiary.activities.DiaryMainActivity
import me.blog.korn123.easydiary.fragments.SettingsScheduleFragment
import me.blog.korn123.easydiary.helper.*
import me.blog.korn123.easydiary.models.Alarm
import me.blog.korn123.easydiary.receivers.AlarmReceiver
import me.blog.korn123.easydiary.views.FixedCardView
import me.blog.korn123.easydiary.views.FixedTextView
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.pow


/**
 * Created by CHO HANJOONG on 2018-02-06.
 * This code based 'Simple-Commons' package
 * You can see original 'Simple-Commons' from below link.
 * https://github.com/SimpleMobileTools/Simple-Commons
 */

val Context.config: Config get() = Config.newInstance(this)

fun Context.isNightMode() = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
    Configuration.UI_MODE_NIGHT_YES -> false
    Configuration.UI_MODE_NIGHT_NO -> false
    else -> false
}

fun Context.pauseLock() {
    if (config.aafPinLockEnable || config.fingerprintLockEnable) {

        // FIXME remove test code
//        Toast.makeText(this, "${this::class.java.simpleName}", Toast.LENGTH_LONG).show()
        config.aafPinLockPauseMillis = System.currentTimeMillis()
    }
}

fun Context.updateTextColors(viewGroup: ViewGroup, tmpTextColor: Int = 0, tmpAccentColor: Int = 0) {
    if (isNightMode()) return

    val textColor = if (tmpTextColor == 0) baseConfig.textColor else tmpTextColor
    val backgroundColor = baseConfig.backgroundColor
    val accentColor = if (tmpAccentColor == 0) {
        if (isBlackAndWhiteTheme()) {
            Color.WHITE
        } else {
            baseConfig.primaryColor
        }
    } else {
        tmpAccentColor
    }

    val cnt = viewGroup.childCount
    (0 until cnt)
            .map { viewGroup.getChildAt(it) }
            .forEach {
                when (it) {
                    is MyTextView -> it.setColors(textColor, accentColor, backgroundColor)
                    is FixedTextView -> {
                        if (it.applyGlobalColor) it.setColors(textColor, accentColor, backgroundColor)
                    }
                    is MyAppCompatSpinner -> it.setColors(textColor, accentColor, backgroundColor)
                    is MySwitchCompat -> it.setColors(textColor, accentColor, backgroundColor)
//                    is MyCompatRadioButton -> it.setColors(textColor, accentColor, backgroundColor)
//                    is MyAppCompatCheckbox -> it.setColors(textColor, accentColor, backgroundColor)
                    is MyEditText -> {
                        it.setTextColor(textColor)
                        it.setHintTextColor(textColor.adjustAlpha(0.5f))
                        it.setLinkTextColor(accentColor)
                    }
                    is MyFloatingActionButton -> it.backgroundTintList = ColorStateList.valueOf(accentColor)
                    is MySeekBar -> it.setColors(textColor, accentColor, backgroundColor)
                    is MyButton -> it.setColors(textColor, accentColor, backgroundColor)
                    is ModalView -> it.setBackgroundColor(accentColor)
                    is ViewGroup -> updateTextColors(it, textColor, accentColor)
                }
            }
}

fun Context.updateAppViews(viewGroup: ViewGroup, tmpBackgroundColor: Int = 0) {
    if (isNightMode()) return

    val backgroundColor = if (tmpBackgroundColor == 0) baseConfig.backgroundColor else tmpBackgroundColor
    val cnt = viewGroup.childCount
    (0 until cnt)
            .map { viewGroup.getChildAt(it) }
            .forEach {
                when (it) {
                    is CardView -> {
                        if (it !is FixedCardView) it.setCardBackgroundColor(backgroundColor)
                        updateAppViews(it)
                    }
                    is ViewGroup -> updateAppViews(it)
                    is RadioButton -> {
                        it.run {
                            setTextColor(config.textColor)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                buttonTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)), intArrayOf(
                                        config.textColor,
                                        config.textColor
                                ))
                            }
                        }
                    }
                    is CheckBox -> {
                        it.run {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                buttonTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)), intArrayOf(
                                        config.textColor,
                                        config.textColor
                                ))
                            }
                        }
                    }
                    is SwitchCompat -> {
                        it.run {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && config.primaryColor == config.backgroundColor) {
                                trackTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)), intArrayOf(
                                        ColorUtils.setAlphaComponent(config.textColor, 190),
                                        config.textColor
                                ))
                                thumbTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)), intArrayOf(
                                        ColorUtils.setAlphaComponent(config.textColor, 255),
                                        config.textColor
                                ))
                            }
                        }
                    }
                }
            }
}

fun Context.updateCardViewPolicy(viewGroup: ViewGroup) {
    if (isNightMode()) return

    val cnt = viewGroup.childCount
    (0 until cnt)
            .map { viewGroup.getChildAt(it) }
            .forEach {
                when (it) {
                    is CardView -> {
                        if (config.enableCardViewPolicy || (it is FixedCardView && it.fixedAppcompatPadding)) {
                            it.useCompatPadding = true
                            it.cardElevation = CommonUtils.dpToPixelFloatValue(this, 2F)
                        } else {
                            it.useCompatPadding = false
                            it.cardElevation = 0F
                        }

                        updateCardViewPolicy(it)
                    }
                    is ViewGroup -> updateCardViewPolicy(it)
                }
            }
}

fun Context.updateTextSize(viewGroup: ViewGroup, context: Context, addSize: Int) {
    if (isNightMode()) return

    val cnt = viewGroup.childCount
    val settingFontSize: Float = config.settingFontSize + addSize
    (0 until cnt)
            .map { index -> viewGroup.getChildAt(index) }
            .forEach {
                when (it) {
                    is TextView -> {
                        it.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingFontSize)
                    }
                    is ViewGroup -> updateTextSize(it, context, addSize)
                }
            }
}

fun Context.initTextSize(viewGroup: ViewGroup) {
    if (isNightMode()) return

    val cnt = viewGroup.childCount
    val defaultFontSize: Float = CommonUtils.dpToPixelFloatValue(this, DEFAULT_FONT_SIZE_SUPPORT_LANGUAGE.toFloat())
    val settingFontSize: Float = config.settingFontSize
    (0 until cnt)
            .map { index -> viewGroup.getChildAt(index) }
            .forEach {
                when (it) {
                    is me.blog.korn123.easydiary.views.CalendarItem -> {
                        if (config.settingCalendarFontScale != DEFAULT_CALENDAR_FONT_SCALE) {
                            it.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingFontSize * config.settingCalendarFontScale)
                        }
                    }
                    is FixedTextView -> {
                        if (it.applyGlobalSize) it.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingFontSize)
                    }
                    is TextView -> { 
                        when (it.id) {
                            R.id.contentsLength -> it.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingFontSize * 0.8F)
                            R.id.symbolTextArrow -> {}
                            R.id.createdDate -> {}
                            else -> it.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingFontSize)
                        }
                    }
                    is ViewGroup -> initTextSize(it)
                }
            }
}

fun Context.initTextSize(textView: TextView) {
    if (isNightMode()) return

    val defaultFontSize: Float = CommonUtils.dpToPixelFloatValue(this, DEFAULT_FONT_SIZE_SUPPORT_LANGUAGE.toFloat())
    val settingFontSize: Float = config.settingFontSize
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingFontSize)
}

fun Context.updateDrawableColorInnerCardView(resourceId: Int) {
    if (isNightMode()) return

    changeDrawableIconColor(config.textColor, resourceId)
}

fun Context.updateAlertDialog(alertDialog: AlertDialog, message: String? = null, customView: View? = null, customTitle: String? = null) {
    alertDialog.run {
        when (customView == null) {
            true -> {
                val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val messageView = inflater.inflate(R.layout.dialog_message, null).apply {
                    simpleMessage.text = message
                    if (this is ViewGroup) {
                        this.setBackgroundColor(config.backgroundColor)
                        initTextSize(this)
                        updateTextColors(this)
                        updateAppViews(this)
                        FontUtils.setFontsTypeface(this@updateAlertDialog, this@updateAlertDialog.assets, null, this)
                    }
                }
                setView(messageView)
            }
            false -> setView(customView)
        }
//        if (!isNightMode()) window?.setBackgroundDrawable(ColorDrawable(baseConfig.backgroundColor))
        if (!isNightMode()) window?.setBackgroundDrawable(GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(config.backgroundColor)
            cornerRadius = CommonUtils.dpToPixelFloatValue(this@updateAlertDialog, 3F)
        })

        val globalTypeface = FontUtils.getCommonTypeface(this@updateAlertDialog)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        customTitle?.let {
            val titleView = TextView(this@updateAlertDialog).apply {
                text = customTitle
                if (!isNightMode()) setTextColor(baseConfig.textColor)
//                setBackgroundColor(ContextCompat.getColor(this@updateAlertDialog, R.color.white))
                typeface = globalTypeface
                val padding = CommonUtils.dpToPixel(this@updateAlertDialog, 15F)
                setPadding(padding * 2, padding, padding * 2, padding)
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18F)
//                        setBackgroundColor(resources.getColor(android.R.color.white))
            }
            setCustomTitle(titleView)
        }
        show()
        getButton(AlertDialog.BUTTON_POSITIVE).run {
            if (!isNightMode()) setTextColor(baseConfig.textColor)
            typeface = globalTypeface
        }
        getButton(AlertDialog.BUTTON_NEGATIVE).run {
            if (!isNightMode()) setTextColor(baseConfig.textColor)
            typeface = globalTypeface
        }
        if (!isNightMode()) getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(baseConfig.textColor)
    }
}

fun Context.changeDrawableIconColor(color: Int, resourceId: Int) {
    AppCompatResources.getDrawable(this, resourceId)?.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN);
        } else {
            setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }
}

fun Context.checkPermission(permissions: Array<String>): Boolean {
    val listDeniedPermissions: List<String> = permissions.filter { permission -> 
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED
    }
    return listDeniedPermissions.isEmpty()
}

fun Context.preferencesContains(key: String): Boolean {
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    return preferences.contains(key)
}

fun Context.applyFontToMenuItem(mi: MenuItem) {
    val mNewTitle = SpannableString(mi.title)
    mNewTitle.setSpan(CustomTypefaceSpan("", FontUtils.getCommonTypeface(this)!!), 0, mNewTitle.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    mi.title = mNewTitle
}

fun Context.getUriForFile(targetFile: File): Uri {
    val authority = "${this.packageName}.provider"
    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) FileProvider.getUriForFile(this, authority, targetFile) else Uri.fromFile(targetFile)
}

fun Context.createTemporaryPhotoFile(uri: Uri? = null, fromUri: Boolean = false): File {
    val temporaryFile = File(EasyDiaryUtils.getApplicationDataDirectory(this) + DIARY_PHOTO_DIRECTORY, CAPTURE_CAMERA_FILE_NAME)
    if (temporaryFile.exists()) temporaryFile.delete()

    when (fromUri) {
        true -> {
            val inputStream = contentResolver.openInputStream(uri!!)
            IOUtils.copy(inputStream, FileOutputStream(temporaryFile.absoluteFile))
            IOUtils.closeQuietly(inputStream)
        }
        false -> temporaryFile.createNewFile()
    }

    return temporaryFile
}

fun Context.preferenceToJsonString(): String {
    var jsonString: String = ""
    val preferenceMap: HashMap<String, Any> = hashMapOf()

    // Settings Basic
    preferenceMap[PRIMARY_COLOR] = config.primaryColor
    preferenceMap[BACKGROUND_COLOR] = config.backgroundColor
    preferenceMap[SETTING_CARD_VIEW_BACKGROUND_COLOR] = config.screenBackgroundColor
    preferenceMap[TEXT_COLOR] = config.textColor
    preferenceMap[SETTING_THUMBNAIL_SIZE] = config.settingThumbnailSize
    preferenceMap[SETTING_CONTENTS_SUMMARY] = config.enableContentsSummary
    preferenceMap[SETTING_SUMMARY_MAX_LINES] = config.summaryMaxLines
    preferenceMap[ENABLE_CARD_VIEW_POLICY] = config.enableCardViewPolicy
    preferenceMap[SETTING_MULTIPLE_PICKER] = config.multiPickerEnable
    preferenceMap[DIARY_SEARCH_QUERY_CASE_SENSITIVE] = config.diarySearchQueryCaseSensitive
    preferenceMap[SETTING_CALENDAR_START_DAY] = config.calendarStartDay
    preferenceMap[SETTING_CALENDAR_SORTING] = config.calendarSorting
    preferenceMap[SETTING_COUNT_CHARACTERS] = config.enableCountCharacters
    preferenceMap[HOLD_POSITION_ENTER_EDIT_SCREEN] = config.holdPositionEnterEditScreen

    // Settings font
    preferenceMap[SETTING_FONT_NAME] = config.settingFontName
    preferenceMap[LINE_SPACING_SCALE_FACTOR] = config.lineSpacingScaleFactor
    preferenceMap[SETTING_FONT_SIZE] = config.settingFontSize
    preferenceMap[SETTING_CALENDAR_FONT_SCALE] = config.settingCalendarFontScale
    preferenceMap[SETTING_BOLD_STYLE] = config.boldStyleEnable

    // Settings Lock
    preferenceMap[APP_LOCK_ENABLE] = config.aafPinLockEnable
    preferenceMap[APP_LOCK_SAVED_PASSWORD] = config.aafPinLockSavedPassword

    val gson = GsonBuilder().setPrettyPrinting().create()
    jsonString = gson.toJson(preferenceMap)
    return jsonString
}

fun Context.shareFile(targetFile: File) {
    shareFile(targetFile, contentResolver.getType(getUriForFile(targetFile)) ?: MIME_TYPE_BINARY)
}

fun Context.shareFile(targetFile: File, mimeType: String) {
    Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, getUriForFile(targetFile))
        type = mimeType
        startActivity(Intent.createChooser(this, getString(R.string.diary_card_share_info)))
    }
}

fun Context.exportRealmFile() {
    val srcFile = File(EasyDiaryDbHelper.getRealmPath())
    val destFilePath = BACKUP_DB_DIRECTORY + DIARY_DB_NAME + "_" + DateUtils.getCurrentDateTime("yyyyMMdd_HHmmss")
    val destFile = File(EasyDiaryUtils.getApplicationDataDirectory(this) + destFilePath)
    FileUtils.copyFile(srcFile, destFile, false)
    config.diaryBackupLocal = System.currentTimeMillis()
}

fun Context.formatTime(showSeconds: Boolean, use24HourFormat: Boolean, hours: Int, minutes: Int, seconds: Int): String {
    val hoursFormat = if (use24HourFormat) "%02d" else "%01d"
    var format = "$hoursFormat:%02d"

    return if (showSeconds) {
        format += ":%02d"
        String.format(format, hours, minutes, seconds)
    } else {
        String.format(format, hours, minutes)
    }
}

fun Context.getFormattedTime(passedSeconds: Int, showSeconds: Boolean, makeAmPmSmaller: Boolean): SpannableString {
    val use24HourFormat = config.use24HourFormat
    val hours = (passedSeconds / 3600) % 24
    val minutes = (passedSeconds / 60) % 60
    val seconds = passedSeconds % 60

    return if (!use24HourFormat) {
        val formattedTime = formatTo12HourFormat(showSeconds, hours, minutes, seconds)
        val spannableTime = SpannableString(formattedTime)
        val amPmMultiplier = if (makeAmPmSmaller) 0.4f else 1f
        spannableTime.setSpan(RelativeSizeSpan(amPmMultiplier), spannableTime.length - 5, spannableTime.length, 0)
        spannableTime
    } else {
        val formattedTime = formatTime(showSeconds, use24HourFormat, hours, minutes, seconds)
        SpannableString(formattedTime)
    }
}

fun Context.formatTo12HourFormat(showSeconds: Boolean, hours: Int, minutes: Int, seconds: Int): String {
    val appendable = getString(if (hours >= 12) R.string.p_m else R.string.a_m)
    val newHours = if (hours == 0 || hours == 12) 12 else hours % 12
    return "${formatTime(showSeconds, false, newHours, minutes, seconds)} $appendable"
}

fun Context.isScreenOn() = (getSystemService(Context.POWER_SERVICE) as PowerManager).isScreenOn

fun Context.getOpenAlarmTabIntent(alarm: Alarm): PendingIntent {
    val intent: Intent? = when (alarm.workMode) {
        Alarm.WORK_MODE_DIARY_WRITING -> {
            Intent(this, DiaryInsertActivity::class.java).apply {
                putExtra(DIARY_EXECUTION_MODE, EXECUTION_MODE_ACCESS_FROM_OUTSIDE)
            }
        }
        Alarm.WORK_MODE_DIARY_BACKUP_LOCAL -> {
            Intent(this, DiaryMainActivity::class.java)
        }
        else -> null
    }
    return PendingIntent.getActivity(this, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT)
}

fun Context.getAlarmIntent(alarm: Alarm): PendingIntent {
    val intent = Intent(this, AlarmReceiver::class.java)
    intent.putExtra(SettingsScheduleFragment.ALARM_ID, alarm.id)
    return PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
}

fun Context.cancelAlarmClock(alarm: Alarm) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(getAlarmIntent(alarm))
}

fun Context.scheduleNextAlarm(alarm: Alarm, showToast: Boolean) {
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    for (i in 0..7) {
        val currentDay = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val isCorrectDay = alarm.days and 2.0.pow(currentDay).toInt() != 0
        val currentTimeInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        if (isCorrectDay && (alarm.timeInMinutes > currentTimeInMinutes || i > 0)) {
            val triggerInMinutes = alarm.timeInMinutes - currentTimeInMinutes + (i * DAY_MINUTES)
            setupAlarmClock(alarm, triggerInMinutes * 60 - calendar.get(Calendar.SECOND))

            if (showToast) {
                showRemainingTimeMessage(triggerInMinutes)
            }
            break
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

fun Context.setupAlarmClock(alarm: Alarm, triggerInSeconds: Int) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val targetMS = System.currentTimeMillis() + triggerInSeconds * 1000
    AlarmManagerCompat.setAlarmClock(alarmManager, targetMS, getOpenAlarmTabIntent(alarm), getAlarmIntent(alarm))
}

fun Context.showRemainingTimeMessage(totalMinutes: Int) {
    val fullString = String.format(getString(R.string.alarm_goes_off_in), formatMinutesToTimeString(totalMinutes))
    toast(fullString, Toast.LENGTH_LONG)
}

fun Context.showAlarmNotification(alarm: Alarm) {
    val pendingIntent = getOpenAlarmTabIntent(alarm)
    val notification = getAlarmNotification(pendingIntent, alarm)
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(alarm.id, notification)

    when (alarm.workMode) {
        Alarm.WORK_MODE_DIARY_BACKUP_LOCAL -> exportRealmFile()
    }

    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    if (isScreenOn()) {
        scheduleNextAlarm(alarm, true)
    } else {
        scheduleNextAlarm(alarm, false)
        powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "myApp:notificationLock").apply {
            acquire(3000)
        }
    }
}

fun Context.rescheduleEnabledAlarms() {
    EasyDiaryDbHelper.readAlarmAll().forEach {
        if (it.isEnabled) scheduleNextAlarm(it, false)
    }
}

@SuppressLint("NewApi")
fun Context.getAlarmNotification(pendingIntent: PendingIntent, alarm: Alarm): Notification {
    if (isOreoPlus()) {
        // Create the NotificationChannel
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("${NOTIFICATION_CHANNEL_ID}_alarm", "${NOTIFICATION_CHANNEL_NAME}_alarm", importance)
        channel.description = NOTIFICATION_CHANNEL_DESCRIPTION
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(applicationContext, "${NOTIFICATION_CHANNEL_ID}_alarm")
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_easydiary)
            .setLargeIcon(when (alarm.workMode) {
                Alarm.WORK_MODE_DIARY_WRITING -> BitmapFactory.decodeResource(resources, R.drawable.ic_diary_writing)
                Alarm.WORK_MODE_DIARY_BACKUP_LOCAL -> BitmapFactory.decodeResource(resources, R.drawable.ic_diary_backup_local)
                else -> null
            })
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentTitle("Easy Diary schedule notification")
            .setContentText(alarm.label)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

    val notification = builder.build()
//    notification.flags = notification.flags or Notification.FLAG_INSISTENT
    return notification
}

@ColorInt
@SuppressLint("ResourceAsColor")
fun Context.getColorResCompat(@AttrRes id: Int): Int {
    val resolvedAttr = TypedValue()
    theme.resolveAttribute(id, resolvedAttr, true)
    val colorRes = resolvedAttr.run { if (resourceId != 0) resourceId else data }
    return ContextCompat.getColor(this, colorRes)
}

fun Context.changeBitmapColor(drawableResourceId: Int, color: Int): Bitmap {
    val drawable = AppCompatResources.getDrawable(this, drawableResourceId)
    val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
        } else {
            setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
    }
    return bitmap
}
