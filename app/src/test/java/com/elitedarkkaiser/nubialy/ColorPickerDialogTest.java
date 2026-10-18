package com.elitedarkkaiser.nubialy;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class ColorPickerDialogTest {
    @Test public void hexSynchronizesRgbAndPreviewWithoutCommittingUntilConfirmed(){
        var controller=Robolectric.buildActivity(AppCompatActivity.class);var activity=controller.get();activity.setTheme(R.style.Theme_NubiaLy);controller.setup();int[] selected={0};
        var dialog=ColorPickerDialog.show(activity,"Custom clock color","12:34",0xffc0a8ff,color->selected[0]=color);var root=dialog.getWindow().getDecorView();TextInputEditText hex=root.findViewWithTag("color-picker-hex");
        assertEquals("#C0A8FF",hex.getText().toString());hex.setText("#123456");assertEquals(18f,((Slider)root.findViewWithTag("color-picker-red")).getValue(),0f);assertEquals(52f,((Slider)root.findViewWithTag("color-picker-green")).getValue(),0f);assertEquals(86f,((Slider)root.findViewWithTag("color-picker-blue")).getValue(),0f);assertEquals(0xff123456,((TextView)root.findViewWithTag("color-picker-preview")).getCurrentTextColor());assertEquals(0,selected[0]);
        hex.setText("#1234");dialog.getButton(-1).performClick();assertTrue(dialog.isShowing());assertNotNull(((TextInputLayout)root.findViewWithTag("color-picker-field")).getError());assertEquals(0,selected[0]);hex.setText("abcdef");dialog.getButton(-1).performClick();assertEquals(0xffabcdef,selected[0]);assertFalse(dialog.isShowing());controller.pause().stop().destroy();
    }
    @Test public void activityDestructionDismissesUnconfirmedPicker(){
        var controller=Robolectric.buildActivity(AppCompatActivity.class);var activity=controller.get();activity.setTheme(R.style.Theme_NubiaLy);controller.setup();int[] calls={0};var dialog=ColorPickerDialog.show(activity,"Custom label color","App label",0xffc0a8ff,color->calls[0]++);controller.pause().stop().destroy();assertFalse(dialog.isShowing());assertEquals(0,calls[0]);
    }
}
