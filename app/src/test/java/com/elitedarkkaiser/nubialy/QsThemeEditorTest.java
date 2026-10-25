package com.elitedarkkaiser.nubialy;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class QsThemeEditorTest {
 @Test public void headerHasSeparateBatteryPercentagePickerAndNativeReset(){var controller=Robolectric.buildActivity(AppCompatActivity.class);var a=controller.get();a.setTheme(R.style.Theme_NubiaLy);controller.setup();var draft=new SurfaceStyle.QsTheme[]{SurfaceStyle.QsTheme.STOCK};var parent=new LinearLayout(a);parent.setOrientation(1);a.setContentView(parent);var editor=new QsThemeEditor(a,()->draft[0],value->draft[0]=value,()->{});editor.header(parent);var control=parent.findViewWithTag("qs-color:23");assertNotNull(control);control.performClick();var dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();TextInputEditText hex=dialog.getWindow().getDecorView().findViewWithTag("color-picker-hex");hex.setText("123456");assertEquals(0,draft[0].batteryTextColor());dialog.getButton(-1).performClick();assertEquals(0xff123456,draft[0].batteryTextColor());assertEquals(0,draft[0].statusColor());var row=(android.view.ViewGroup)control.getParent();row.getChildAt(row.getChildCount()-1).performClick();assertEquals(0,draft[0].batteryTextColor());controller.pause().stop().destroy();}
 @Test public void qsColorsUseSharedHexEditorAndKeepOtherDraftFields(){
  var controller=Robolectric.buildActivity(AppCompatActivity.class);var a=controller.get();a.setTheme(R.style.Theme_NubiaLy);controller.setup();
  var draft=new SurfaceStyle.QsTheme[]{SurfaceStyle.QsTheme.STOCK.with(9,1).with(10,0xff223344)};var parent=new LinearLayout(a);a.setContentView(parent);
  var editor=new QsThemeEditor(a,()->draft[0],value->draft[0]=value,()->{});editor.colors(parent);
  parent.findViewWithTag("qs-color:0").performClick();var dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(dialog);
  TextInputEditText hex=dialog.getWindow().getDecorView().findViewWithTag("color-picker-hex");hex.setText("123456");assertEquals(0,draft[0].activeBackground());dialog.getButton(-1).performClick();assertEquals(0xff123456,draft[0].activeBackground());assertEquals(1,draft[0].header());assertEquals(0xff223344,draft[0].headerColor());
  controller.pause().stop().destroy();
 }
}
