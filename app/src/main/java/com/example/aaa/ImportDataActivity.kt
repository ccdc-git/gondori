package com.example.aaa

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_import.*
import java.time.LocalDate

class ImportDataActivity : AppCompatActivity() {

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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)


        //군별 선택
        val species_adapter = ArrayAdapter.createFromResource(this,R.array.species_array,android.R.layout.simple_spinner_dropdown_item)
        species_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        TextView_inp_species.adapter = species_adapter


        //입대일 선택
        val listener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
                TextView_startDate_btn.text = "%d.%02d.%02d".format(year, month + 1, dayOfMonth)
            }
        }
        TextView_startDate_btn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                Log.v("onClick", "yes")
                val dpd = DatePickerDialog(
                    this@ImportDataActivity,
                    listener,
                    LocalDate.now().year,
                    LocalDate.now().monthValue,
                    LocalDate.now().dayOfMonth
                )
                dpd.show()
            }
        })


                Button_start.setOnClickListener{v: View? ->
                    if (dataCheck()) {
                        val intent = Intent(this, MainActivity::class.java)
                        val pref = getSharedPreferences("preference", MODE_PRIVATE)
                        val editor = pref.edit()
                        editor.putString("inp_user_name", TextView_inp_user_name.text.toString())
                        editor.putString("inp_species", TextView_inp_species.selectedItem.toString())
                        editor.putString("inp_start_date", TextView_startDate_btn.text.toString())
                        editor.apply()
                        setResult(Activity.RESULT_OK, intent)
                        finish()
            }
        }
    }
    fun dataCheck() : Boolean{

        return true
    }

}