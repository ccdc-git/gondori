package com.example.aaa

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.*
import com.example.aaa.R.string.*
import java.time.LocalDateTime
import kotlinx.android.synthetic.main.activity_main.*
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit


class MainActivity : AppCompatActivity() {

    var isRunning = true
    var handler : Handler? = null
    var today: LocalDate = LocalDate.now()

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
            this.startActivityForResult(intent, 1)
        }
        else{
            whenStart()
            handler = Handler()
            mThread().run()
        }

        //메뉴버튼(초기화, 변경)
        Button_menu.setOnClickListener {
            val context = ContextThemeWrapper(this,R.style.PopupMenu)
            val popupMenu = PopupMenu(context,Button_menu)
            popupMenu.menuInflater.inflate(R.menu.my_menu,popupMenu.menu)
            popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                if (item != null) {
                    isRunning = false  //다른 화면 넘어갈때 쓰레드 멈춤
                    if (item.itemId == R.id.menu_clear) {
                        Log.v("menu","Clear Data")
                        clearPref()
                        val intent2 : Intent = Intent(this@MainActivity,ImportDataActivity::class.java)
                        startActivityForResult(intent2,1)
                    }else if (item.itemId == R.id.change_date) {
                        Log.v("menu","change_date")
                        val intent2 : Intent = Intent(this@MainActivity,ChangeDataActivity::class.java)
                        startActivityForResult(intent2,2)
                    }
                    return@OnMenuItemClickListener true
                } else return@OnMenuItemClickListener false
            })
            popupMenu.show()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) { //값 불러오기
            if(resultCode == Activity.RESULT_OK){
                Log.d("is_RESULT_OK", "import_OK")
                val pref = getSharedPreferences("preference",MODE_PRIVATE)
                MyOnClicked.initialize(
                    this,
                    pref.getString("inp_user_name","윤종선"),
                    pref.getString("inp_species", "의무소방"),
                    LocalDateTime.parse(pref.getString("inp_start_date","2018-08-09T14:00:00")),
                    "preference"
                )  //기본값 계산
                whenStart()
                isRunning = true //쓰레드 다시 시작
                mThread().run()
            }else {
                Log.d("is_RESULT_OK","import canceled")
                finish()
            }
        } else if (requestCode == 2) {  //값 수정
            if(resultCode == Activity.RESULT_OK) {
                Log.d("is_RESULT_OK", "change_OK")
                whenStart()
                isRunning = true //쓰레드 다시 시작
                mThread().run()
            }else {
                Log.d("is_RESULT_OK","Change canceled")
                isRunning = true //쓰레드 다시 시작
                mThread().run()
            }
        }
        handler = Handler()
        mThread().run()
        Log.d("is_RESULT_OK", resultCode.toString())
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



        //단축일 계산
        var service_months = serviceMonths(species)
        var reduce_dates = calReduceDates(start_datetime,species)
        if (species == "공군") {
            if (reduce_dates > 60) {
                reduce_dates = 0
                service_months -= 2
            }
        }
        else{
            if(reduce_dates > 90){
                reduce_dates = 0
                service_months -= 3
            }
        }

        val now_datetime = LocalDateTime.now()

        //current_class_int  0:입대전 1:이방 2:일방 3:상방 4:수방 5:민간인
        val current_class_int = when {
            now_datetime < start_datetime -> 0
            now_datetime < first_upgrade -> 1
            now_datetime < second_upgrade -> 2
            now_datetime < third_upgrade -> 3
            now_datetime < finish_datetime -> 4
            else -> 5
        }

        // 입대전(0), 민간인(5)처리
        val notConscripted = now_datetime < start_datetime
        val finishConscripted = start_datetime < now_datetime

        //

        val next_class_int = current_class_int + 1
        val current_class = classes(species)[current_class_int]
        val next_class = classes(species)[next_class_int]

        val due_finish_dates = if(finishConscripted) 0
        else ChronoUnit.DAYS.between(now_datetime.toLocalDate(),finish_datetime.toLocalDate()).toInt()

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

        val current_seniority = if(now_datetime.toLocalDate() < start_datetime.toLocalDate().plusMonths(1).withDayOfMonth(1))  1
                        else  (ChronoUnit.MONTHS.between(current_class_datetime.minusDays(1).plusMonths(1).withDayOfMonth(1).toLocalDate(),now_datetime.toLocalDate()) + 1).toInt()

        val next_month_firstday = now_datetime.plusMonths(1).withDayOfMonth(1).toLocalDate()

        var next_month_seniority = 0
        var next_month_class_int = 0
        if (next_month_firstday >= next_class_datetime.toLocalDate()) { //다음 달 1일이 진급일이면
            next_month_seniority = 1
            next_month_class_int = next_class_int
        }
        else{
            next_month_seniority = current_seniority + 1
            next_month_class_int = current_class_int
        }
        val next_month_class = classes(species)[next_month_class_int]

        val next_seniority_datetime = when {
            notConscripted -> now_datetime.plusDays(1)
            finishConscripted -> now_datetime.minusDays(1)
            else ->
                if (next_month_firstday >= finish_datetime.toLocalDate() && now_datetime.toLocalDate() <= finish_datetime.toLocalDate()) finish_datetime
                else LocalDateTime.of(next_month_firstday, LocalTime.of(0, 0, 0))
        }
        val current_seniority_datetime = when {
            notConscripted -> now_datetime.plusDays(1)
            finishConscripted -> now_datetime.minusDays(1)
            else ->
                if (now_datetime.toLocalDate() < start_datetime.toLocalDate().plusMonths(1).withDayOfMonth(1)) start_datetime
                else now_datetime.withDayOfMonth(1).with(LocalTime.of(0, 0, 0))
        }

        val current_service_dates : Int = when {
            notConscripted -> 0
            finishConscripted -> total_service_dates
            else -> ChronoUnit.DAYS.between(start_datetime.toLocalDate(),now_datetime.toLocalDate()).toInt()
        }

        val due_next_class_dates = if(finishConscripted) 0
        else ChronoUnit.DAYS.between(now_datetime.toLocalDate(),next_class_datetime.toLocalDate())

        val classes_image_resorce = if (species == "의무경찰") if (current_class_int in 1..4) R.mipmap.police
                                                                    else null
                                else if (species == "의무소방") when(current_class_int) {
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

        if(notConscripted || finishConscripted){
            TextView_D_day.text = ""
            TextView_current_seniority.text = current_class

            TextView_nextSeniority.text = ""
            TextView_nextSeniority_date.text = ""
        }
        else{
            TextView_D_day.text = getText(R.string.d_day).toString().format(due_finish_dates)
            TextView_current_seniority.text = getText(seniority).toString().format(current_class,current_seniority)

            TextView_nextSeniority.text = getText(seniority).toString().format(next_month_class,next_month_seniority)
            TextView_nextSeniority_date.text = getText(date_dot).toString().format(next_seniority_datetime.year,next_seniority_datetime.monthValue,next_seniority_datetime.dayOfMonth)
        }

        if (classes_image_resorce != null){
        ImageView_classes_image.setImageResource(classes_image_resorce)
        }


        TextView_progressbar_finishDate.text = getText(date_korean).toString().format(finish_datetime.year,finish_datetime.monthValue,finish_datetime.dayOfMonth)


        //TextView_nextSeniority.text = getText(seniority).toString().format(next_month_class,next_month_seniority)
        //TextView_nextSeniority_date.text = getText(date_dot).toString().format(next_seniority_datetime.year,next_seniority_datetime.monthValue,next_seniority_datetime.dayOfMonth)


        TextView_nextClasses.text = next_class
        TextView_nextClasses_date.text = getText(date_dot).toString().format(next_class_datetime.year,next_class_datetime.monthValue,next_class_datetime.dayOfMonth)


        TextView_summery_totalDate.text = total_service_dates.toString()
        TextView_summery_currentDate.text = current_service_dates.toString()
        TextView_summery_nextClassesDate.text = due_next_class_dates.toString()
        TextView_summery_restDate.text = due_finish_dates.toString()
        if (notConscripted){
            TextView_summery_nextClassesDate_title.text = "언제가고"
            TextView_summery_restDate_title.text = "언제오나"
        }else{
            TextView_summery_nextClassesDate_title.text = "다음 진급일"
            TextView_summery_restDate_title.text = "남은 복무일"

        }



        //실시간 갱신 부분을 위해 저장
        val editor = pref.edit()
        editor.putString("current_seniority_datetime",current_seniority_datetime.toString())
        editor.putString("next_seniority_datetime",next_seniority_datetime.toString())
        editor.putString("current_class_datetime",current_class_datetime.toString())
        editor.putString("next_class_datetime",next_class_datetime.toString())
        editor.apply()

        Log.v("current_seniority_datetime",current_seniority_datetime.toString())
        Log.v("next_seniority_datetime",next_seniority_datetime.toString())
        Log.v("current_class_datetime",current_class_datetime.toString())
        Log.v("next_class_datetime",next_class_datetime.toString())
        Log.v("reduce_dates",reduce_dates.toString())
        Log.v("first_upgrade",first_upgrade.toString())
        Log.v("second_upgrade",second_upgrade.toString())
        Log.v("third_upgrade",third_upgrade.toString())
        Log.v("finish_datetime",finish_datetime.toString())
        Log.v("total_service_dates",total_service_dates.toString())
        Log.v("current_seniority",current_seniority.toString())
        Log.v("next_month_seniority",next_month_seniority.toString())
    }



    inner class mThread : Thread(){
        override fun run() {
            //전체 진행율
            val total_progress = refreshProgress("start_datetime","finish_datetime")
            progress_horizontal_total.progress = (total_progress * 1000000000).toInt()
            (View_totalProgress.layoutParams as LinearLayout.LayoutParams).weight = (total_progress * 100).toFloat()
            TextView_Progressbar_totalProgress.text = "%.7f%%".format(total_progress * 100)
            //다음 호봉 진행율
            val next_seniority_progress = refreshProgress("current_seniority_datetime","next_seniority_datetime")
            ProgressBar_nextSeniority.progress = (next_seniority_progress * 1000000000).toInt()
            TextView_nextSeniority_progress.text = "%.6f%%".format(next_seniority_progress*100)
            (View_nextSeniority.layoutParams as LinearLayout.LayoutParams).weight = ((next_seniority_progress*100).toFloat())
            //다음 계급 진행율
            val next_class_progress = refreshProgress("current_class_datetime","next_class_datetime")
            ProgressBar_nextClasses.progress = (next_class_progress * 1000000000).toInt()
            TextView_nextClasses_progress.text = "%.6f%%".format(next_class_progress*100)
            (View_nextClasses.layoutParams as LinearLayout.LayoutParams).weight = ((next_class_progress*100).toFloat())
            //계속 할건지 판단
            if(isRunning){
                sleep(1)
                handler?.post(this)
                if (today.compareTo(LocalDate.now()) != 0){ //날짜 변경시 다시 계산
                    today = LocalDate.now()
                    whenStart()
                }
            }
        }
        private fun refreshProgress(startKey : String, finishKey : String) : Double { //진행율 갱신
            val pref = getSharedPreferences("preference",MODE_PRIVATE)
            try {
                val startDatetime =
                    LocalDateTime.parse(pref.getString(startKey, "fail_$startKey"))
                val finishDatetime =
                    LocalDateTime.parse(pref.getString(finishKey, "fail_$finishKey"))
                val nowDatetime = LocalDateTime.now()
                var totalProgress = ChronoUnit.MILLIS.between(startDatetime,nowDatetime).toDouble() / ChronoUnit.MILLIS.between(startDatetime,finishDatetime)
                if (0 > totalProgress){
                    totalProgress = 0.0
                }else if(totalProgress > 1 ) {
                    totalProgress = 1.0
                }
                return totalProgress
            }catch (e: DateTimeException){
                Log.d("Exception","fail to get datetime in refreshProgress($startKey, $finishKey)")
            }
            return 0.0
        }
    }


        override fun onDestroy() {
            isRunning = false
            super.onDestroy()
    }

    private fun classes(species: String?) : List<String> = when(species){
        "의무소방" -> getText(fire).split("_")
        "의무경찰" -> getText(police).split("_")
        else -> getText(soldier).split("_")
    }
    private fun serviceMonths(species: String?) : Int = when(species){
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
        editor.apply()
    }

    private fun calReduceDates(startDateTime: LocalDateTime , species: String?) : Int{
        /*
        단축일 계산
        2018-10-01전역자 기준으로 시작해서 2주에 1일씩 단축
         */
        val reduceStandard = LocalDateTime.parse("2018-10-01T00:00:00").minusMonths(serviceMonths(species).toLong()).plusDays(2)
        return if(startDateTime < reduceStandard) 0
        else (ChronoUnit.DAYS.between(reduceStandard,startDateTime) / 14 + 1).toInt()
    }

}
