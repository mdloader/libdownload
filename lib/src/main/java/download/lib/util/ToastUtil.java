package download.lib.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class ToastUtil {

    /**
     * Shows a custom toast with a specific color and rounded corners.
     * @param context - The application context.
     * @param message - The message to display in the toast.
     * @param isSuccess - Boolean to indicate if the toast should be green (success) or red (error).
     */
    public static void showCustomToast(Context context, String message, boolean isSuccess) {
        // Create the toast
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);

        // Get the TextView inside the toast to modify its appearance
        TextView toastMessage = toast.getView().findViewById(android.R.id.message);

        // Create a GradientDrawable to apply rounded corners and background color
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(context, 26));  // Set corner radius to 12dp

        if (isSuccess) {
            // Green color for success
            drawable.setColor(Color.parseColor("#4CAF50"));  // Green background
            toastMessage.setTextColor(Color.WHITE);
        } else {
            // Red color for error
            drawable.setColor(Color.parseColor("#F44336"));  // Red background
            toastMessage.setTextColor(Color.WHITE);
        }

        // Apply the drawable to the toast's background
        toast.getView().setBackground(drawable);

        // Optionally set the toast's position
        toast.setGravity(Gravity.BOTTOM, 0, 200);  // Bottom of the screen

        // Show the toast
        toast.show();
    }

    /**
     * Converts dp (density-independent pixels) to pixels.
     * @param context - The application context.
     * @param dp - The value in dp to convert to pixels.
     * @return The equivalent value in pixels.
     */
    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }
}

