package xyz.goodistory.xxperiment_android.coroutine

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.goodistory.xxperiment_android.databinding.ActivityCoroutineTestBinding
import java.lang.Exception


class CoroutineTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCoroutineTestBinding

    companion object {
        const val LOG_TAG: String = "CoroutineTestActivity"

        private fun currentThread(): String {
            return Thread.currentThread().toString() + ", "
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCoroutineTestBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        binding.coroutineTestButtonSeries.setOnClickListener {
            Log.d(LOG_TAG, currentThread() + "直列: out of launch start")
            lifecycleScope.launch {
                Log.d(LOG_TAG, currentThread() + "直列: in launch start")
                val googleId: String = ApiService.getGoogleId()
                val imageUrl: String = ApiService.getPostNumberFromGoogle(googleId)
                val imageData: String = ApiService.registerPostNumber(imageUrl)
                Log.d(LOG_TAG, currentThread() + "直列: in launch end, value=$imageData")
            }
            Log.d(LOG_TAG, currentThread() + "直列: out of launch end")
        }


        binding.coroutineTestButtonParallel.setOnClickListener {
            Log.d(LOG_TAG, currentThread() + "並列: out of launch start")
            lifecycleScope.launch {

                binding.coroutineTestTextResult.text = "loading..."

                Log.d(LOG_TAG, currentThread() + "並列: in launch start")

                val userId1: Deferred<String> = async { ApiService.getCustomerName(1) }
                val userId2: Deferred<String> = async { ApiService.getCustomerName(2) }
                val userId3: Deferred<String> = async { ApiService.getCustomerName(5) }

                val text: String = ApiService.generateText(userId1.await(), userId2.await())
                binding.coroutineTestTextResult.text = text
            }
            Log.d(LOG_TAG, currentThread() + "並列: out of launch end")
        }


        binding.threadTestButton.setOnClickListener {
            GetGoogleIdTask.getGroupId(
                {groupId: String ->
                    Log.d(LOG_TAG, currentThread() + "onSuccess, groupId: $groupId" + groupId)
                    binding.coroutineTestTextResult.text = groupId

                },
                {
                    Log.d(LOG_TAG, currentThread() + "onFailure")
                }
            )
        }

    }

    class GetGoogleIdTask(
        private val onSuccess: (String) -> Unit,
        private val onFailure: () -> Unit

    ) : AsyncTask<Void, Void, String?>() {

        companion object {
            fun getGroupId(onSuccess: (String) -> Unit, onFailure: () -> Unit) {
                GetGoogleIdTask(onSuccess, onFailure).execute()
            }
        }

        override fun doInBackground(vararg params: Void?): String? {
            Log.d(LOG_TAG, currentThread() + "start")

            val rtn: String?  = try {
                Thread.sleep(2000)
                (0..9).random().toString()
            } catch (e: Exception) {
                null
            }
            Log.d(LOG_TAG, currentThread() + "end")

            return rtn
        }

        override fun onPostExecute(groupId: String?) {
            if (groupId == null) {
                onFailure()
            } else {
                onSuccess(groupId)
            }
        }

    }

    class ApiService {
        companion object {

            suspend fun getGoogleId(): String {
                Log.d(LOG_TAG, currentThread() + "call getGoogleId() start")
                delay(1000)
                val googleId = (0..9).random().toString()
                Log.d(LOG_TAG, currentThread() + "call getGoogleId() end, googleId: $googleId")

                return googleId
            }

            suspend fun getPostNumberFromGoogle(googleId: String): String {
                Log.d(LOG_TAG, currentThread() + "getPostNumberFromGoogle() start, googleId: $googleId")
                delay(1000)
                val phoneNumber = "$googleId$googleId$googleId-$googleId$googleId$googleId$googleId"
                Log.d(LOG_TAG, currentThread() + "getPostNumberFromGoogle() end, phoneNumber: $phoneNumber")

                return phoneNumber
            }

            suspend fun registerPostNumber(postNumber: String): String {
                Log.d(LOG_TAG, currentThread() + "registerPostNumber() start, postNumber: $postNumber")
                delay(1000)
                val message = "郵便番号:$postNumber 登録成功"
                Log.d(LOG_TAG, currentThread() + "registerPostNumber() end, message: $message")

                return message
            }

            suspend fun getCustomerName(customerId: Long): String {
                Log.d(LOG_TAG, currentThread() + "getCustomerName() start, customerId: $customerId")
                delay(customerId * 1000)
                val customerName = "${customerId}の名前"
                Log.d(LOG_TAG, currentThread() + "getCustomerName() end, customerName: $customerName")

                return customerName
            }

            suspend fun generateText(name1: String, name2: String): String {
                Log.d(LOG_TAG, currentThread() + "generateText() start, name1: $name1, name2: $name2")

                delay(1000)
                val text = "「${name1}」より「${name2}」様へ・・・"
                Log.d(LOG_TAG, currentThread() + "generateText() end, text: $text")

                return text
            }
        }

    }
}