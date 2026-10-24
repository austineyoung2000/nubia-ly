package com.elitedarkkaiser.nubialy.xposed;
import android.view.View;
import java.util.*;
/** Only headers obtained from the inspected QS container qualify. */
final class QsHeaderScope {
 static final Set<View> headers=Collections.newSetFromMap(new WeakHashMap<>());
 static boolean isRoot(View v){return headers.contains(v);}
 static boolean contains(View v){for(int i=0;i<24&&v!=null;i++){if(isRoot(v))return true;v=v.getParent() instanceof View parent?parent:null;}return false;}
}
