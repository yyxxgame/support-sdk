package com.yyxx.support.demo

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import cn.yyxx.support.AppUtils
import cn.yyxx.support.device.DeviceInfoUtils
import cn.yyxx.support.hawkeye.LogUtils
import cn.yyxx.support.msa.MsaDeviceIdsHandler
import cn.yyxx.support.volley.VolleySingleton
import cn.yyxx.support.volley.source.Response
import cn.yyxx.support.volley.source.toolbox.ImageRequest
import com.tencent.mmkv.MMKV

/**
 * @author #Suyghur.
 * Created on 2021/04/23
 */
class DemoActivity : Activity(), View.OnClickListener {

    private val events = mutableListOf(
        Item(0, "00 获取MSA DeviceIds"),
        Item(1, "是否安装微信"),
        Item(2, "获取网络图片"),
        Item(3, "显示浮标"),
        Item(4, "隐藏浮标"),
        Item(5, "MMKV测试 encode"),
        Item(6, "MMKV测试 decode")
    )

    private lateinit var textView: TextView
    private lateinit var imgView: ImageView
    private lateinit var demoFloatView: FloatView
    private val sb = StringBuilder()
    private var hasReadIds = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initDeviceInfo()
    }

    private fun initView() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        textView = TextView(this)
        layout.addView(textView)
        for (event in events) {
            with(Button(this)) {
                id = event.id
                tag = event.id
                text = event.name
                setOnClickListener(this@DemoActivity)
                layout.addView(this)
            }
        }
//        val gifView = GifView(this)
//        gifView.setGifResource(ResUtils.getResId(this, "test", "drawable"))
//        layout.addView(gifView)
        imgView = ImageView(this)
        layout.addView(imgView)
        val scrollView = ScrollView(this)
        scrollView.addView(layout)
        setContentView(scrollView)
        FloatViewServiceManager.getInstance().init(this)
    }

    private fun initDeviceInfo() {
        sb.append("Android ID : ").append(DeviceInfoUtils.getAndroidDeviceId(this)).append("\n")
        sb.append("手机制造商 : ").append(DeviceInfoUtils.getDeviceManufacturer()).append("\n")
        sb.append("手机品牌 : ").append(DeviceInfoUtils.getDeviceBrand()).append("\n")
        sb.append("手机型号 : ").append(DeviceInfoUtils.getDeviceModel()).append("\n")
        sb.append("CPU核数 : ").append(DeviceInfoUtils.getCpuCount()).append("\n")
        sb.append("CPU架构 : ").append(DeviceInfoUtils.getCpuAbi()).append("\n")
        sb.append("本机运行内存Ram : ").append(DeviceInfoUtils.getDeviceRam()).append("\n")
        sb.append("本应用可用运行内存Ram : ").append(DeviceInfoUtils.getAppAvailRam(this)).append("\n")
        textView.text = sb.toString()
    }

    private fun requestImg() {
        val url = "https://i.loli.net/2019/09/16/oMtIUKWiavEbFPw.jpg"
        val request = object : ImageRequest(url,
            Response.Listener<Bitmap> {
                imgView.setImageBitmap(it)
            },
            0, 0, Bitmap.Config.ARGB_8888,
            Response.ErrorListener {
                LogUtils.e("onError")
            }
        ) {

        }
        VolleySingleton.getInstance(this.applicationContext).addToRequestQueue(this.applicationContext, request)
    }

    override fun onClick(v: View?) {
        v?.apply {
            when (tag as Int) {
                0 -> {
                    if (!hasReadIds) {
                        sb.append("OAID : ").append(MsaDeviceIdsHandler.oaid).append("\n")
                        sb.append("VAID : ").append(MsaDeviceIdsHandler.vaid).append("\n")
                        sb.append("AAID : ").append(MsaDeviceIdsHandler.aaid).append("\n")
                        textView.text = sb.toString()
                        hasReadIds = true
                    }
                }
                1 -> {
                    LogUtils.d("aaaaa : ${AppUtils.isPackageInstalled(this@DemoActivity, "com.tencent.mm")}")
                }
                2 -> requestImg()
                3 -> FloatViewServiceManager.getInstance().attach()
                4 -> FloatViewServiceManager.getInstance().detach()
                5 -> {
                    MMKV.defaultMMKV()!!.encode("test", "yyxx support")
                    MMKV.defaultMMKV()!!.encode("test1", "yyxx support1")
                    MMKV.defaultMMKV()!!.encode("test2", "yyxx support2")
                    MMKV.defaultMMKV()!!.encode("test3", "yyxx support3")

                }
                6 -> {
//                    sb.append("MMKV decode : ").append(MMKV.defaultMMKV()!!.decodeString("test"))
//                    textView.text = sb.toString()
                    val keys = MMKV.defaultMMKV()!!.allKeys()
                    keys?.apply {
                        for (key in this) {
                            LogUtils.i(key)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FloatViewServiceManager.getInstance().attach()
    }

    override fun onPause() {
        super.onPause()
        FloatViewServiceManager.getInstance().detach()
    }

    override fun onDestroy() {
        super.onDestroy()
        FloatViewServiceManager.getInstance().release()

    }
}