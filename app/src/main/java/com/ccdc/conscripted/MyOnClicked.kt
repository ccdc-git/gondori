package com.ccdc.conscripted
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import android.content.Context


class MyOnClicked : AppCompatActivity() {
    companion object{

        fun initialize(context: Context , userName: String?, species: String?,startDateTime: LocalDateTime , prefName: String) {
            val pref = context.getSharedPreferences(prefName,MODE_PRIVATE)
            val editor = pref.edit()
            //이름, 프로필, 군종, 단축일, 전체복무일, 전역일
            //진급일
            var service_months = serviceMonths(species)

            //단축일 계산
            var reduce_dates = calReduceDates(startDateTime,species)
            if (species == "공군") {
                if (reduce_dates > 60) {
                    reduce_dates = 0
                    service_months -= 2
                }
            }else{
                if(reduce_dates > 90){
                    reduce_dates = 0
                    service_months -= 3
                }
            }

            //진급일 계산
            val first_upgrade = upgradeDateTime(startDateTime,2)
            val second_upgrade = upgradeDateTime(first_upgrade,6)
            val third_upgrade = upgradeDateTime(second_upgrade,6)
            //전역일 계산
            val finish_datetime = startDateTime.plusMonths(service_months.toLong()).minusDays(reduce_dates.toLong()+1).withHour(9)
            //전체복무일 계산
            val total_service_dates = ChronoUnit.DAYS.between(startDateTime.toLocalDate(),finish_datetime.toLocalDate())

            editor.putString("user_name",userName)
            editor.putString("species",species)
            editor.putString("start_datetime",startDateTime.toString())
            editor.putInt("service_months",service_months)
            editor.putInt("reduce_dates",reduce_dates)
            editor.putString("first_upgrade",first_upgrade.toString())
            editor.putString("second_upgrade",second_upgrade.toString())
            editor.putString("third_upgrade",third_upgrade.toString())
            editor.putString("finish_datetime",finish_datetime.toString())
            editor.putInt("total_service_dates",total_service_dates.toInt())
            editor.putBoolean("is_first",false)
            editor.apply()


            Log.v("reduce_dates",reduce_dates.toString())
            Log.v("first_upgrade",first_upgrade.toString())
            Log.v("second_upgrade",second_upgrade.toString())
            Log.v("third_upgrade",third_upgrade.toString())
            Log.v("finish_datetime",finish_datetime.toString())
            Log.v("total_service_dates",total_service_dates.toString())
        }

        private fun upgradeDateTime(startDatetime : LocalDateTime, months : Long) : LocalDateTime{
            //start_datetime : 현재진급 날짜
            //months : 2 6 6 기준
            val classesStandard = LocalDate.parse("2019-09-01")
            return if (startDatetime.toLocalDate().plusMonths(months) < classesStandard) {
                startDatetime.minusDays(1).plusMonths(months + 2).withDayOfMonth(1).withHour(0)
            } else {
                startDatetime.minusDays(1).plusMonths(months+1).withDayOfMonth(1).withHour(0)
            }
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
        private fun serviceMonths(species: String?) : Int = when(species){
            "의무소방" -> 23
            "해양경찰" -> 23
            "공군" -> 24
            else -> 21
        }
    }

}