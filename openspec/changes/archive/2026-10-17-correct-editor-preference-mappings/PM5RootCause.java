package smoke;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.equinox.app.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.application.*;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntSourceViewerConfiguration;

/** Test-only root-cause experiment; no product/internal-store access is added. */
public class PM5RootCause implements IApplication {
  static Throwable failure;
  static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS "+message);}
  static TextAttribute pdeAttribute(XMLConfiguration config)throws Exception{
    return (TextAttribute)field(config,"fXMLCommentAttr");
  }
  static Object field(Object object,String name)throws Exception{
    for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException e){}
    throw new NoSuchFieldException(name);
  }
  public Object start(IApplicationContext context)throws Exception{
    Display display=PlatformUI.createDisplay();
    try{PlatformUI.createAndRunWorkbench(display,new WorkbenchAdvisor(){
      public String getInitialWindowPerspectiveId(){return "org.eclipse.ui.resourcePerspective";}
      public boolean openWindows(){return true;}
      public void initialize(IWorkbenchConfigurer c){super.initialize(c);c.setExitOnLastWindowClose(false);c.setSaveAndRestore(false);}
      public void postStartup(){display.asyncExec(()->{try{probe(display);}catch(Throwable e){failure=e;e.printStackTrace();}finally{PlatformUI.getWorkbench().close();}});}
    });}finally{display.dispose();}
    return failure==null?0:1;
  }
  public void stop(){}
  static void probe(Display display)throws Exception{
    System.out.println("PM5 ROOT CAUSE Platform="+Platform.getBundle("org.eclipse.platform").getVersion()+" PDE="+Platform.getBundle("org.eclipse.pde.ui").getVersion()+" Ant="+Platform.getBundle("org.eclipse.ant.ui").getVersion());
    Shell parent=new Shell(display);
    var pdeStore=PDEPlugin.getDefault().getPreferenceStore();
    var antStore=AntUIPlugin.getDefault().getPreferenceStore();
    IEclipsePreferences pdePrefs=InstanceScope.INSTANCE.getNode("org.eclipse.pde.ui");
    IEclipsePreferences antPrefs=InstanceScope.INSTANCE.getNode("org.eclipse.ant.ui");
    String pdeBase="editor.color.xml_comment", antBase="org.eclipse.ant.ui.commentsColor";
    pdePrefs.putBoolean(pdeBase+"_bold",false);pdePrefs.putBoolean(pdeBase+"_italic",false);
    antPrefs.putBoolean(antBase+"_bold",false);antPrefs.putBoolean(antBase+"_italic",false);
    var colors=new org.eclipse.pde.internal.ui.editor.text.ColorManager();
    var pde=new XMLConfiguration(colors);
    var ant=new AntSourceViewerConfiguration();
    SourceViewer pdeViewer=new SourceViewer(parent,null,SWT.NONE),antViewer=new SourceViewer(parent,null,SWT.NONE);
    pdeViewer.setDocument(new Document("<!-- XML -->"));antViewer.setDocument(new Document("<!-- XML -->"));
    pde.getPresentationReconciler(pdeViewer);ant.getPresentationReconciler(antViewer);
    List<String> eventTypes=new ArrayList<>();
    IPropertyChangeListener pdeListener=e->{eventTypes.add(e.getProperty()+"="+e.getNewValue()+" ["+e.getNewValue().getClass().getName()+"]");pde.adaptToPreferenceChange(e);};
    IPropertyChangeListener antListener=ant::adaptToPreferenceChange;
    List<IStatus> failures=new ArrayList<>();
    ILogListener logListener=(status,plugin)->{if(status.getException()!=null)failures.add(status);};
    Platform.addLogListener(logListener);
    pdeStore.addPropertyChangeListener(pdeListener);antStore.addPropertyChangeListener(antListener);
    try{
      pdePrefs.put(pdeBase,"69,103,137");
      check(pdeAttribute(pde).getForeground().getRGB().equals(new RGB(69,103,137)),"PDE color is correct before suffix notifications");
      pdePrefs.putBoolean(pdeBase+"_bold",true);
      check((pdeAttribute(pde).getStyle()&SWT.BOLD)==0,"reproduced PDE raw String true leaves supported bold disabled");
      System.out.println("RAW PDE EVENT "+eventTypes.get(eventTypes.size()-1));

      // Positive control: the actual existing store emits typed events on its own listeners.
      pdeStore.setValue(pdeBase+"_bold",false);pdeStore.setValue(pdeBase+"_bold",true);
      check((pdeAttribute(pde).getStyle()&SWT.BOLD)!=0,"actual PDE store Boolean event enables bold");
      System.out.println("ACTUAL STORE EVENT "+eventTypes.get(eventTypes.size()-1));

      ScopedPreferenceStore separate=new ScopedPreferenceStore(InstanceScope.INSTANCE,"org.eclipse.pde.ui");
      List<String> ownEvents=new ArrayList<>();
      IPropertyChangeListener own=e->ownEvents.add(e.getNewValue().getClass().getName());
      separate.addPropertyChangeListener(own);
      try{separate.setValue(pdeBase+"_bold",false);separate.setValue(pdeBase+"_bold",true);}
      finally{separate.removePropertyChangeListener(own);}
      check(pdeStore.getBoolean(pdeBase+"_bold")&&(pdeAttribute(pde).getStyle()&SWT.BOLD)==0,"separate public store does not deliver its Boolean event to existing PDE listeners");
      System.out.println("SEPARATE STORE own="+ownEvents+" actual PDE="+eventTypes.get(eventTypes.size()-1));

      pdePrefs.putBoolean(pdeBase+"_underline",false);
      check(pdeAttribute(pde).getForeground().getRGB().equals(new RGB(0,0,0)),"reproduced PDE unsupported underline suffix substitutes black");
      antPrefs.put(antBase,"69,103,137");antPrefs.putBoolean(antBase+"_bold",true);
      var antAttribute=(TextAttribute)field(ant,"xmlCommentAttribute");
      check((antAttribute.getStyle()&SWT.BOLD)!=0,"Ant supported bold accepts the raw String true");
      int before=failures.size();antPrefs.putBoolean(antBase+"_underline",false);
      check(failures.size()>before,"reproduced Ant unsupported underline boolean reaches color parser and logs exception");
      for(int i=before;i<failures.size();i++)System.out.println("ANT ERROR "+failures.get(i).getException());

      check(Platform.getPlugin("org.eclipse.pde.ui")==null,"deprecated public Platform.getPlugin is a null stub on 2024-09");
      check(Platform.getBundle("org.eclipse.pde.ui").adapt(IPreferenceStore.class)==null,"PDE bundle does not adapt to public IPreferenceStore");
      int storeServices=0;
      var services=Platform.getBundle("org.eclipse.pde.ui").getRegisteredServices();
      if(services!=null)for(var service:services){Object classes=service.getProperty("objectClass");if(java.util.Arrays.asList((String[])classes).contains(IPreferenceStore.class.getName()))storeServices++;}
      check(storeServices==0,"PDE does not register an IPreferenceStore service");
      System.out.println("PM5 ROOT CAUSE COMPLETE (expected defects confirmed; no product fix)");
    }finally{
      pdeStore.removePropertyChangeListener(pdeListener);antStore.removePropertyChangeListener(antListener);
      Platform.removeLogListener(logListener);colors.dispose();parent.dispose();
    }
  }
}
