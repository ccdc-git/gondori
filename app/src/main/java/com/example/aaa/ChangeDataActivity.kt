package com.example.aaa

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_change.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ChangeDataActivity : AppCompatActivity() {
    var is_RESULT_OK = 0
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change)

        initiate("preference") //초기값 설정

        //끝내기 버튼
        Button_change_start.setOnClickListener{v: View? ->
                    if (dataCheck()) {
                        saveChanges()
                        is_RESULT_OK = 1
                        val intent = Intent(this, MainActivity::class.java)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
            }
        }
    }

    private fun initiate(prefName: String){
        val pref = getSharedPreferences(prefName,MODE_PRIVATE)
        val user_name = pref.getString("user_name","fail_user_name").toString()
        val species= pref.getString("species","fail_species").toString()
        val start_datetime= LocalDateTime.parse(pref.getString("start_datetime","fail_start_datetime"))
        val first_upgrade = LocalDateTime.parse(pref.getString("first_upgrade","fail_first_upgrade"))
        val second_upgrade = LocalDateTime.parse(pref.getString("second_upgrade","fail_second_upgrade"))
        val third_upgrade = LocalDateTime.parse(pref.getString("third_upgrade","fail_third_upgrade"))
        val finish_datetime = LocalDateTime.parse(pref.getString("finish_datetime","fail_finish_datetime"))

        //user_name
        TextView_change_user_name.setText(user_name)

        //군별 선택
        val species_adapter = ArrayAdapter.createFromResource(this,R.array.species_array,R.layout.my_spinner_layout)
        species_adapter.setDropDownViewResource(R.layout.my_spinner_dropdown)
        Spinner_change_species.adapter = species_adapter
        Spinner_change_species.setSelection(species_adapter.getPosition(species)) //초기값 설정

        //입대일 선택
        setDatePicker(TextView_change_startDate_dp,start_datetime)
        //일병진급일 선택
        setDatePicker(TextView_change_first_upgrade_dp,first_upgrade)
        //상병진급일 선택
        setDatePicker(TextView_change_second_upgrade_dp,second_upgrade)
        //병장진급일 선택
        setDatePicker(TextView_change_third_upgrade_dp,third_upgrade)
        //전역일 선택
        setDatePicker(TextView_change_finish_datetime_dp,finish_datetime)

    }

    private fun dataCheck() : Boolean{
        val start_datetime = LocalDateTime.of(LocalDate.parse(TextView_change_startDate_dp.text,formatter),LocalTime.of(14,0,0))
        val first_upgrade = LocalDateTime.of(LocalDate.parse(TextView_change_first_upgrade_dp.text,formatter),LocalTime.of(0,0,0))
        val second_upgrade = LocalDateTime.of(LocalDate.parse(TextView_change_second_upgrade_dp.text,formatter),LocalTime.of(0,0,0))
        val third_upgrade = LocalDateTime.of(LocalDate.parse(TextView_change_third_upgrade_dp.text,formatter),LocalTime.of(0,0,0))
        val finish_datetime = LocalDateTime.of(LocalDate.parse(TextView_change_finish_datetime_dp.text,formatter),LocalTime.of(9,0,0))

        if(start_datetime > first_upgrade || first_upgrade > second_upgrade|| second_upgrade > third_upgrade || third_upgrade > finish_datetime){
            val mySnackbar  =  Snackbar.make(LinearLayout_change,"진급(전역)날짜가 잘못되었습니다.",2000)
            mySnackbar.setAction("자동완성", View.OnClickListener {
                onClickAction()
            })
            mySnackbar.show()
            return false
        }
        return true
    }

    private fun onClickAction() {
        MyOnClicked.initialize(
            this,
            TextView_change_user_name.text.toString(),
            Spinner_change_species.selectedItem.toString(),
            LocalDateTime.of(LocalDate.parse(TextView_change_startDate_dp.text,formatter),LocalTime.of(14,0,0)),
            "temp"
        )
        initiate("temp")//refresh
        getSharedPreferences("temp", Context.MODE_PRIVATE).edit().clear().apply()
        Log.v("refresh","refresh")
    }
    private fun setDatePicker(textView: TextView, currentDateTime: LocalDateTime){
        textView.text = "%d.%02d.%02d".format(currentDateTime.year, currentDateTime.monthValue,currentDateTime.dayOfMonth)//초기값 설정
        textView.setOnClickListener {
            val currentDate = LocalDate.parse(textView.text,formatter)
            val dpd = DatePickerDialog(
                this@ChangeDataActivity,
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth -> textView.text = "%d.%02d.%02d".format(year, month + 1, dayOfMonth) },
                currentDate.year,
                currentDate.monthValue-1,
                currentDate.dayOfMonth
            )
            dpd.show()
        }
    }

    private fun saveChanges() : Boolean{
        val pref = getSharedPreferences("preference",MODE_PRIVATE)
        val editor = pref.edit()

        val start_datetime = LocalDateTime.of(LocalDate.parse(TextView_change_startDate_dp.text,formatter),LocalTime.of(14,0,0))
        val first_upgrade = LocalDateTime.of(LocalDate.parse(TextView_change_first_upgrade_dp.text,formatter),LocalTime.of(0,0,0))
        val second_upgrade = LocalDateTime.of(LocalDate.parse(TextView_change_second_upgrade_dp.text,formatter),LocalTime.of(0,0,0))
        val third_upgrade = LocalDateTime.of(LocalDate.parse(TextView_change_third_upgrade_dp.text,formatter),LocalTime.of(0,0,0))
        val finish_datetime = LocalDateTime.of(LocalDate.parse(TextView_change_finish_datetime_dp.text,formatter),LocalTime.of(9,0,0))

        //전체 복무일
        val total_service_dates = ChronoUnit.DAYS.between(start_datetime.toLocalDate(),finish_datetime.toLocalDate())

        editor.putString("user_name",TextView_change_user_name.text.toString())
        editor.putString("species",Spinner_change_species.selectedItem.toString())
        editor.putString("start_datetime",start_datetime.toString())
        editor.putString("first_upgrade",first_upgrade.toString())
        editor.putString("second_upgrade",second_upgrade.toString())
        editor.putString("third_upgrade",third_upgrade.toString())
        editor.putString("finish_datetime",finish_datetime.toString())
        editor.putInt("total_service_dates",total_service_dates.toInt())
        editor.putBoolean("is_first",false)
        editor.apply()
        return true

    }


    override fun onDestroy() {
        val intent = Intent(this, MainActivity::class.java)
        if(is_RESULT_OK != 1) setResult(Activity.RESULT_CANCELED,intent)
        super.onDestroy()
    }


    //포커스 해제해주는 부분
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action  == MotionEvent.ACTION_DOWN){
            val current_focus = currentFocus
            if(current_focus is EditText){
                var outRect = Rect()
                current_focus.getGlobalVisibleRect(outRect)
                if(!outRect.contains(ev.getRawX().toInt() , ev.getRawY().toInt())){
                    current_focus.clearFocus()
                    val imm : InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(current_focus.windowToken,0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}