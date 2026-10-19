package smoke;

import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import org.eclipse.equinox.app.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.ui.IWorkbench;

/** Real platform-only availability checks, with no JDT/PDE requirements in this bundle. */
public class AbsentIntegrations implements IApplication {
  static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("PASS "+message);}
  static Map<String,String> snapshot(IEclipsePreferences prefs)throws Exception{
    Map<String,String> result=new TreeMap<>();for(String key:prefs.keys())result.put(key,prefs.get(key,null));return result;
  }
  @SuppressWarnings("unchecked") public Object start(IApplicationContext context)throws Exception{
    RuntimeState.verify();
    check(Platform.getBundle("org.eclipse.jdt.ui")==null,"JDT absent in disposable configuration");
    check(Platform.getBundle("org.eclipse.pde.ui")==null,"PDE absent in disposable configuration");
    var product=Platform.getBundle("com.github.eclipsethemes");
    var parser=product.loadClass("com.github.eclipsethemes.core.parser.XmlThemeParser");
    Object theme=parser.getMethod("parse",InputStream.class,File.class).invoke(parser.getConstructor().newInstance(),
      new ByteArrayInputStream("<colorTheme><background color='#102030'/><selectionBackground color='#456789'/><sourceHoverBackground color='#abcdef'/></colorTheme>".getBytes(java.nio.charset.StandardCharsets.UTF_8)),null);
    List<String> errors=new ArrayList<>();
    ILog log=(ILog)Proxy.newProxyInstance(ILog.class.getClassLoader(),new Class<?>[]{ILog.class},(p,m,a)->{if(m.getName().equals("error"))errors.add(String.valueOf(a[0]));return null;});
    var registryClass=product.loadClass("com.github.eclipsethemes.eclipse.AdapterRegistry");
    Object registry=registryClass.getConstructor(ILog.class).newInstance(log);registryClass.getMethod("discover").invoke(registry);
    Map<Object,String> adapters=(Map<Object,String>)registryClass.getMethod("getAdapters").invoke(registry);
    check(adapters.size()==6&&adapters.containsValue("org.eclipse.jdt.ui"),"existing registry contributions retained without changing discovery semantics");
    IEclipsePreferences jdt=InstanceScope.INSTANCE.getNode("org.eclipse.jdt.ui"),pde=InstanceScope.INSTANCE.getNode("org.eclipse.pde.ui");
    jdt.put("unrelated","keep");pde.put("unrelated","keep");var beforeJdt=snapshot(jdt);var beforePde=snapshot(pde);
    var serviceClass=product.loadClass("com.github.eclipsethemes.eclipse.ThemeService");
    var service=serviceClass.getConstructor(registryClass,ILog.class).newInstance(registry,log);
    serviceClass.getMethod("applyTheme",IWorkbench.class,theme.getClass()).invoke(service,null,theme);
    check(beforeJdt.equals(snapshot(jdt)),"absent JDT receives no hover/semantic preference mutation");
    check(beforePde.equals(snapshot(pde)),"absent PDE receives no preference mutation");
    check(errors.isEmpty(),"unavailable integrations are skipped without application errors");
    check(InstanceScope.INSTANCE.getNode("org.eclipse.ui.editors").get("AbstractTextEditor.Color.SelectionBackground",null).equals("69,103,137"),"text selection still applies independently of absent JDT/PDE");
    var adapterClass=product.loadClass("com.github.eclipsethemes.eclipse.adapters.editor.PluginEditorThemeAdapter");
    try{adapterClass.getMethod("apply",theme.getClass(),IEclipsePreferences.class).invoke(adapterClass.getConstructor().newInstance(),theme,pde);throw new AssertionError("Missing unavailable-store failure");}
    catch(InvocationTargetException e){check(e.getCause() instanceof org.osgi.service.prefs.BackingStoreException,"direct absent-PDE access reports a checked integration failure");}
    check(beforePde.equals(snapshot(pde)),"absent accessor fails before PDE mutation without substitute store");
    System.out.println("R1 ABSENT COMPLETE");return 0;
  }
  public void stop(){}
}
