package com.example.aaa

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.widget.*
import com.example.aaa.R.string.*
import java.time.LocalDateTime
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class MainActivity : AppCompatActivity() {

    var is_running = true
    var handler : Handler? = null
    var today = LocalDate.now()

    /*
        할것들
        1. pref 값 초기화
         -이름
         -입대일
         -군종
        2. 날짜계산
         - 단축일
         - 디데이
         - 현재호봉, 현재계급
         - 다음호봉(계급) 날짜
         - 퍼센트
        3. 값 입력
         -프로파일 (사진, 이름, 입대일, 전역일, 단축)
         -디데이, 현재호봉
         -pb_total (전역일, progress(1000000000), layout_weight, 퍼센트)
         -nextSeniority nextClasses
         -마무리부분

    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //preference
        val pref = getSharedPreferences("preference",MODE_PRIVATE)
        if (pref.getBoolean("is_first",true)) {
            val intent: Intent = Intent(this, ImportDataActivity::class.java)
            startActivityForResult(intent, 1)
        }
        else{
            whenStart()
            handler = Handler()
            mThread().run()
        }


        Button_menu.setOnClickListener {
            val context = ContextThemeWrapper(this,R.style.PopupMenu)
            val popupMenu = PopupMenu(context,Button_menu)
            popupMenu.menuInflater.inflate(R.menu.my_menu,popupMenu.menu)
            val listener = object : PopupMenu.OnMenuItemClickListener{
                override fun onMenuItemClick(item: MenuItem?): Boolean {
                    return if (item != null) {
                        if (item.itemId == R.id.menu_clear) {
                            Log.v("menu","Clear Data")
                            clearPref()
                            val intent2 : Intent = Intent(this@MainActivity,ImportDataActivity::class.java)
                            startActivityForResult(intent,1)
                        }
                        true
                    } else false
                }
            }
            popupMenu.setOnMenuItemClickListener(listener)

            popupMenu.show()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            Log.d("is_RESULT_OK","OK")
            import_info()
            whenStart()
            handler = Handler()
            val mthread : mThread = mThread()
            mthread.run()
        }
        else{
            Log.d("is_RESULT_OK","not OK")
            finish()
        }
    }

    private fun import_info() {
        val pref = getSharedPreferences("preference",MODE_PRIVATE)
        val editor = pref.edit()
        //이름, 프로필, 군종, 단축일, 전체복무일, 전역일
        //진급일
        var inp_user_name = pref.getString("inp_user_name","윤종선")
        var inp_species  = pref.getString("inp_species", "의무소방")
        var inp_start_date = pref.getString("inp_start_date","2018-08-09T14:00:00")
/*
        var inp_user_name = "윤종선"
        var inp_species  = "의무소방"
        var inp_start_date = "2018-08-09T14:00:00"*/

        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val start_datetime = LocalDateTime.of(LocalDate.parse(inp_start_date,formatter),LocalTime.of(14,0,0))
        Log.v("start_datetimea",start_datetime.toString())

        var service_months = service_months(inp_species)

        val reduce_standard = LocalDateTime.parse("2018-10-01T00:00:00").minusMonths(service_months.toLong()).plusDays(2)
        val classes_standard = LocalDate.parse("2019-09-01")

        //단축일 계산
        var reduce_dates = if(start_datetime.compareTo(reduce_standard)<0) 0
            else (ChronoUnit.DAYS.between(reduce_standard,start_datetime) / 14 + 1).toInt()
        if(reduce_dates > 90){
            reduce_dates = 0
            service_months -= 3
        }

        //진급일 계산
        val first_upgrade = if (start_datetime.toLocalDate().plusMonths(2).compareTo(classes_standard) < 0 ) start_datetime.minusDays(1).plusMonths(4).withDayOfMonth(1).withHour(0)
                            else start_datetime.minusDays(1).plusMonths(3).withDayOfMonth(1).withHour(0)
        val second_upgrade = if (first_upgrade.toLocalDate().plusMonths(6).compareTo(classes_standard) < 0 ) first_upgrade.minusDays(1).plusMonths(8).withDayOfMonth(1).withHour(0)
                            else first_upgrade.minusDays(1).plusMonths(7).withDayOfMonth(1).withHour(0)
        val third_upgrade = if (second_upgrade.toLocalDate().plusMonths(6).compareTo(classes_standard) < 0 ) second_upgrade.minusDays(1).plusMonths(8).withDayOfMonth(1).withHour(0)
                            else second_upgrade.minusDays(1).plusMonths(7).withDayOfMonth(1).withHour(0)
        //전역일 계산
        val finish_datetime = start_datetime.plusMonths(service_months.toLong()).minusDays(reduce_dates.toLong()+1).withHour(9)
        //전체복무일 계산
        val total_service_dates = ChronoUnit.DAYS.between(start_datetime.toLocalDate(),finish_datetime.toLocalDate())

        editor.putString("user_name",inp_user_name)
        editor.putString("species",inp_species)
        editor.putString("start_datetime",start_datetime.toString())
        editor.putInt("service_months",service_months)
        editor.putInt("reduce_dates",reduce_dates)
        editor.putString("first_upgrade",first_upgrade.toString())
        editor.putString("second_upgrade",second_upgrade.toString())
        editor.putString("third_upgrade",third_upgrade.toString())
        editor.putString("finish_datetime",finish_datetime.toString())
        editor.putInt("total_service_dates",total_service_dates.toInt())
        editor.putBoolean("is_first",false)

        editor.apply()

/*
        Log.v("reduce_dates",reduce_dates.toString())
        Log.v("first_upgrade",first_upgrade.toString())
        Log.v("second_upgrade",second_upgrade.toString())
        Log.v("third_upgrade",third_upgrade.toString())
        Log.v("final_datetime",final_datetime.toString())
        Log.v("total_service_dates",total_service_dates.toString())*/
    }

    private fun whenStart() {
        val pref = getSharedPreferences("preference",MODE_PRIVATE)
        val user_name = pref.getString("user_name","fail_user_name").toString()
        val species= pref.getString("species","fail_species").toString()
        val start_datetime= LocalDateTime.parse(pref.getString("start_datetime","fail_start_datetime"))
        val first_upgrade = LocalDateTime.parse(pref.getString("first_upgrade","fail_first_upgrade"))
        val second_upgrade = LocalDateTime.parse(pref.getString("second_upgrade","fail_second_upgrade"))
        val third_upgrade = LocalDateTime.parse(pref.getString("third_upgrade","fail_third_upgrade"))
        val finish_datetime = LocalDateTime.parse(pref.getString("finish_datetime","fail_finish_datetime"))
        val total_service_dates = pref.getInt("total_service_dates",-1)
        val reduce_dates = pref.getInt("reduce_dates",-1)
/*
    current_class   1
    due_finish_dates    1
    current_seniority   1
    total_progress  1
    next_month_seniority    1
    next_month_class    1
    next_seniority_progress 1
    next_class_datetime 1
    next_class  1
    next_class_progress 1
    current_service_dates
    due_next_class_dates
 */
        val now_datetime = LocalDateTime.now()

        //current_class_int  0:입대전 1:이방 2:일방 3:상방 4:수방 5:민간인
        val current_class_int = if(now_datetime.compareTo(start_datetime) < 0) 0
                                else if(now_datetime.compareTo(first_upgrade) < 0) 1
                                else if(now_datetime.compareTo(second_upgrade) < 0) 2
                                else if(now_datetime.compareTo(third_upgrade) < 0) 3
                                else if(now_datetime.compareTo(finish_datetime) < 0) 4
                                else 5
        val next_class_int = current_class_int + 1
        val current_class = classes(species)[current_class_int]
        val next_class = classes(species)[next_class_int]

        val due_finish_dates = ChronoUnit.DAYS.between(now_datetime.toLocalDate(),finish_datetime.toLocalDate())

        val current_class_datetime = when(current_class_int){
            0 -> start_datetime
            1 -> start_datetime
            2 -> first_upgrade
            3 -> second_upgrade
            4 -> third_upgrade
            else -> finish_datetime
        }
        val next_class_datetime = when(next_class_int){
            0 -> start_datetime
            1 -> start_datetime
            2 -> first_upgrade
            3 -> second_upgrade
            4 -> third_upgrade
            else -> finish_datetime
        }

        val current_seniority = if(now_datetime.toLocalDate().compareTo(start_datetime.toLocalDate().plusMonths(1).withDayOfMonth(1))<0)  1
                        else  (ChronoUnit.MONTHS.between(current_class_datetime.minusDays(1).plusMonths(1).withDayOfMonth(1).toLocalDate(),now_datetime.toLocalDate()) + 1).toInt()

        val total_progress = ChronoUnit.MILLIS.between(start_datetime,now_datetime).toDouble() / ChronoUnit.MILLIS.between(start_datetime,finish_datetime)


        val next_month_firstday = now_datetime.plusMonths(1).withDayOfMonth(1).toLocalDate()

        var next_month_seniority = 0
        var next_month_class_int = 0
        var next_serniority_date = null
        if (next_month_firstday.compareTo(next_class_datetime.toLocalDate()) >= 0) { //다음 달 1일이 진급일이면
            next_month_seniority = 1
            next_month_class_int = next_class_int
        }
        else{
            next_month_seniority = current_seniority + 1
            next_month_class_int = current_class_int
        }
        val next_month_class = classes(species)[next_month_class_int]

        val next_seniority_datetime = if(next_month_firstday.compareTo(finish_datetime.toLocalDate()) >= 0 && now_datetime.toLocalDate().compareTo(finish_datetime.toLocalDate()) <= 0) finish_datetime
        else LocalDateTime.of(next_month_firstday, LocalTime.of(0,0,0))
        val current_seniority_datetime = if(now_datetime.toLocalDate().compareTo(start_datetime.toLocalDate().plusMonths(1).withDayOfMonth(1))<0) start_datetime
        else now_datetime.withDayOfMonth(1).with(LocalTime.of(0,0,0))
        val next_seniority_progress = ChronoUnit.MILLIS.between(current_seniority_datetime,now_datetime).toDouble() / ChronoUnit.MILLIS.between(current_seniority_datetime,next_seniority_datetime)

        val next_class_progress= ChronoUnit.MILLIS.between(current_class_datetime,now_datetime).toDouble() / ChronoUnit.MILLIS.between(current_class_datetime,next_class_datetime)

        val current_service_dates = ChronoUnit.DAYS.between(start_datetime.toLocalDate(),now_datetime.toLocalDate())

        val due_next_class_dates = ChronoUnit.DAYS.between(now_datetime.toLocalDate(),next_class_datetime.toLocalDate())

        val classes_image_resorce = if (species.equals("의무경찰")) if (current_class_int in 1..4) R.mipmap.police
                                                                    else null
                                else if (species.equals("의무소방")) when(current_class_int) {
                                    1 -> R.mipmap.fire_1
                                    2 -> R.mipmap.fire_2
                                    3 -> R.mipmap.fire_3
                                    4 -> R.mipmap.fire_4
                                    else -> null
                                }
                                else when(current_class_int) {
                                    1 -> R.mipmap.soldier_1
                                    2 -> R.mipmap.soldier_2
                                    3 -> R.mipmap.soldier_3
                                    4 -> R.mipmap.soldier_4
                                    else -> null
                                }

        TextView_profile_name.text = getText(profile_name).toString().format(current_class,user_name)
        TextView_profile_startDate.text = getText(date_dot).toString().format(start_datetime.year,start_datetime.monthValue,start_datetime.dayOfMonth)
        TextView_profile_finishDate.text = getText(date_dot).toString().format(finish_datetime.year,finish_datetime.monthValue,finish_datetime.dayOfMonth)
        TextView_profile_reduceDate.text = getText(reduce_date).toString().format(reduce_dates)

        TextView_D_day.text = getText(R.string.d_day).toString().format(due_finish_dates)
        if (classes_image_resorce != null){
        ImageView_classes_image.setImageResource(classes_image_resorce)
        }
        TextView_current_seniority.text = getText(seniority).toString().format(current_class,current_seniority)

        TextView_progressbar_finishDate.text = getText(date_korean).toString().format(finish_datetime.year,finish_datetime.monthValue,finish_datetime.dayOfMonth)
        progress_horizontal_total.progress = (total_progress * 1000000000).toInt()
        (View_totalProgress.layoutParams as LinearLayout.LayoutParams).weight = (total_progress * 100).toFloat()
        TextView_Progressbar_totalProgress.text = "%.7f%%".format((total_progress * 100))

        TextView_nextSeniority.text = getText(seniority).toString().format(next_month_class,next_month_seniority)
        TextView_nextSeniority_date.text = getText(date_dot).toString().format(next_seniority_datetime.year,next_seniority_datetime.monthValue,next_seniority_datetime.dayOfMonth)
        ProgressBar_nextSeniority.progress = (next_seniority_progress * 1000000000).toInt()
        TextView_nextSeniority_progress.text = "%.6f".format(next_seniority_progress*100)
        (View_nextSeniority.layoutParams as LinearLayout.LayoutParams).weight = ((next_seniority_progress*100).toFloat())

        TextView_nextClasses.text = next_class
        TextView_nextClasses_date.text = getText(date_dot).toString().format(next_class_datetime.year,next_class_datetime.monthValue,next_class_datetime.dayOfMonth)
        ProgressBar_nextClasses.progress = (next_class_progress * 1000000000).toInt()
        TextView_nextClasses_progress.text = "%.6f".format(next_class_progress*100)
        (View_nextClasses.layoutParams as LinearLayout.LayoutParams).weight = ((next_class_progress*100).toFloat())

        TextView_summery_totalDate.text = total_service_dates.toString()
        TextView_summery_currentDate.text = current_service_dates.toString()
        TextView_summery_nextClassesDate.text = due_next_class_dates.toString()
        TextView_summery_restDate.text = due_finish_dates.toString()




        val editor = pref.edit()
        editor.putString("current_seniority_datetime",current_seniority_datetime.toString())
        editor.putString("next_seniority_datetime",next_seniority_datetime.toString())
        editor.putString("current_class_datetime",current_class_datetime.toString())
        editor.putString("next_class_datetime",next_class_datetime.toString())

        editor.apply()



        Log.v("reduce_datesa",reduce_dates.toString())
        Log.v("first_upgradea",first_upgrade.toString())
        Log.v("second_upgradeaa",second_upgrade.toString())
        Log.v("third_upgradea",third_upgrade.toString())
        Log.v("final_datetimea",finish_datetime.toString())
        Log.v("total_service_datesa",total_service_dates.toString())
        Log.v("current_seniorityddf",current_seniority.toString())
        Log.v("total_progressll",total_progress.toString())
        Log.v("next_month_seniority",next_month_seniority.toString())
        Log.v("next_class_progress",next_class_progress.toString())
    }

    private fun refresh_total_progress() : Double { //진행율 갱신
        val pref = getSharedPreferences("preference",MODE_PRIVATE)
        try {
            val start_datetime =
                LocalDateTime.parse(pref.getString("start_datetime", "fail_start_datetime"))
            val finish_datetime =
                LocalDateTime.parse(pref.getString("finish_datetime", "fail_finish_datetime"))
            val now_datetime = LocalDateTime.now()
            val total_progress = ChronoUnit.MILLIS.between(start_datetime,now_datetime).toDouble() / ChronoUnit.MILLIS.between(start_datetime,finish_datetime)
            return  total_progress

        }catch (e:Exception){
            Log.d("Exception","fail to get datetime in refresh_progress()")
        }
        return 0.0
    }
    private fun refresh_seniority_progress() : Double { //진행율 갱신
        val pref = getSharedPreferences("preference",MODE_PRIVATE)
        try {
            val current_seniority_datetime =
                LocalDateTime.parse(pref.getString("current_seniority_datetime", "fail_current_seniority_datetime"))
            val next_seniority_datetime =
                LocalDateTime.parse(pref.getString("next_seniority_datetime", "fail_next_seniority_datetime"))
            val now_datetime = LocalDateTime.now()
            val next_seniority_progress = ChronoUnit.MILLIS.between(current_seniority_datetime,now_datetime).toDouble() / ChronoUnit.MILLIS.between(current_seniority_datetime,next_seniority_datetime)
            return  next_seniority_progress

        }catch (e:Exception){
            Log.d("Exception","fail to get datetime in refresh_seniority_progress()")
        }
        return 0.0
    }
    private fun refresh_class_progress() : Double { //진행율 갱신
        val pref = getSharedPreferences("preference",MODE_PRIVATE)
        try {
            val current_class_datetime =
                LocalDateTime.parse(pref.getString("current_class_datetime", "fail_current_class_datetime"))
            val next_class_datetime =
                LocalDateTime.parse(pref.getString("next_class_datetime", "fail_next_class_datetime"))
            val now_datetime = LocalDateTime.now()
            val next_class_progress= ChronoUnit.MILLIS.between(current_class_datetime,now_datetime).toDouble() / ChronoUnit.MILLIS.between(current_class_datetime,next_class_datetime)
            return  next_class_progress

        }catch (e:Exception){
            Log.d("Exception","fail to get datetime in refresh_progress()")
        }
        return 0.0
    }


    inner class mThread : Thread(){
        override fun run() {

            val total_progress = refresh_total_progress()
            progress_horizontal_total.progress = (total_progress * 1000000000).toInt()
            (View_totalProgress.layoutParams as LinearLayout.LayoutParams).weight = (total_progress * 100).toFloat()
            TextView_Progressbar_totalProgress.text = "%.7f%%".format(total_progress * 100)

            val next_seniority_progress = refresh_seniority_progress()
            ProgressBar_nextSeniority.progress = (next_seniority_progress * 1000000000).toInt()
            TextView_nextSeniority_progress.text = "%.6f".format(next_seniority_progress*100)
            (View_nextSeniority.layoutParams as LinearLayout.LayoutParams).weight = ((next_seniority_progress*100).toFloat())

            val next_class_progress = refresh_class_progress()
            ProgressBar_nextClasses.progress = (next_class_progress * 1000000000).toInt()
            TextView_nextClasses_progress.text = "%.6f".format(next_class_progress*100)
            (View_nextClasses.layoutParams as LinearLayout.LayoutParams).weight = ((next_class_progress*100).toFloat())




            if(is_running){

                sleep(1)
                handler?.post(this)
                if (today.compareTo(LocalDate.now()) != 0){
                    today = LocalDate.now()
                    whenStart()
                }
            }
        }
    }


        override fun onDestroy() {
            super.onDestroy()
            is_running = false

    }

    fun doCalculate_date() {
        /*
        2. 날짜계산
            - 전역일
            - 단축일  입대일 기준 2017-01-03부터 14일에 1일 단축
            - 디데이
            - 현재호봉, 현재계급
            - 다음호봉(계급) 날짜
            - 퍼센트
        */
        val now_DateTime = LocalDateTime.now()
    }
    fun doWork_date(){

    }

    private fun classes(species: String?) : List<String> = when(species){
        "의무소방" -> getText(fire).split("_")
        "의무경찰" -> getText(police).split("_")
        else -> getText(soldier).split("_")
    }
    private fun service_months(species: String?) : Int = when(species){
        "의무소방" -> 23
        "해양경찰" -> 23
        "공군" -> 24
        else -> 21
    }
    private fun clearPref(){
        val pref = getSharedPreferences("preference",MODE_PRIVATE)
        val editor = pref.edit()
        editor.clear()
        editor.putBoolean("is_first",true)
        editor.commit()
    }









}
