import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.loudnesmeter.R

class ThresholdDialogFragment(private val currentThreshold: Int) : DialogFragment() {

    lateinit var onThresholdSetListener: (Int) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view: View = inflater.inflate(R.layout.threshold_dialog, null)
        val editTextThreshold: EditText = view.findViewById(R.id.editTextThreshold)
        val buttonSet: Button = view.findViewById(R.id.buttonSet)

        editTextThreshold.setText(currentThreshold.toString())

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Configure Threshold")
            .setNegativeButton("Cancel", null)
            .create()

        buttonSet.setOnClickListener {
            val thresholdValue = editTextThreshold.text.toString().toIntOrNull()
            if (thresholdValue != null) {
                onThresholdSetListener.invoke(thresholdValue)
                dialog.dismiss()
            } else {
                editTextThreshold.error = "Invalid threshold value"
            }
        }

        return dialog
    }
}
