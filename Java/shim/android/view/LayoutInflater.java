package android.view;
import android.content.Context;
import android.util.AttributeSet;
import jet.runtime.ResTable;
import jet.runtime.XmlAttrs;
import org.w3c.dom.*;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.WeakHashMap;

public class LayoutInflater {
    public interface Factory { View onCreateView(String name, Context context, AttributeSet attrs); }
    public interface Factory2 extends Factory { View onCreateView(View parent, String name, Context context, AttributeSet attrs); }
    public interface Filter { boolean onLoadClass(Class<?> c); }
    private Factory mFactory; private boolean mFactorySet;
    private Factory2 mFactory2, mPrivateFactory;
    private Filter mFilter;
    // Android semantics: one factory per inflater instance; a clone (cloneInContext) may set its own, merged in front of the inherited one.
    public void setFactory(Factory factory) { if (mFactorySet) throw new IllegalStateException("A factory has already been set on this LayoutInflater"); mFactorySet = true;
        mFactory = mFactory == null ? factory : merge(factory, null, mFactory, mFactory2); }
    public void setFactory2(Factory2 factory) { if (mFactorySet) throw new IllegalStateException("A factory has already been set on this LayoutInflater"); mFactorySet = true;
        if (mFactory == null) { mFactory = mFactory2 = factory; } else { Factory2 m = merge(factory, factory, mFactory, mFactory2); mFactory = mFactory2 = m; } }
    private static Factory2 merge(final Factory f1, final Factory2 f21, final Factory f2, final Factory2 f22) {
        return new Factory2() {
            public View onCreateView(String n, Context c, AttributeSet a) { View v = f1.onCreateView(n, c, a); return v != null ? v : f2.onCreateView(n, c, a); }
            public View onCreateView(View p, String n, Context c, AttributeSet a) { View v = f21 != null ? f21.onCreateView(p, n, c, a) : f1.onCreateView(n, c, a); return v != null ? v : f22 != null ? f22.onCreateView(p, n, c, a) : f2.onCreateView(n, c, a); }
        }; }
    public void setPrivateFactory(Factory2 f) { mPrivateFactory = f; }
    public Factory getFactory() { return mFactory; }
    public Factory2 getFactory2() { return mFactory2; }
    public Filter getFilter() { return mFilter; }
    public void setFilter(Filter f) { mFilter = f; }

    private static final Map<Context, LayoutInflater> CACHE = new WeakHashMap<>();
    private final Context mContext;
    protected LayoutInflater(Context c) { mContext = c; }
    protected LayoutInflater(LayoutInflater original, Context c) { mContext = c; mFactory = original.mFactory; mFactory2 = original.mFactory2; mPrivateFactory = original.mPrivateFactory; }
    public static synchronized LayoutInflater from(Context c) { return CACHE.computeIfAbsent(c, LayoutInflater::new); }
    public LayoutInflater cloneInContext(Context c) { return new LayoutInflater(this, c); }
    public Context getContext() { return mContext; }
    private ResTable table() { return mContext.getResources().table(); }

    public View inflate(int resource, ViewGroup root) { return inflate(resource, root, root != null); }
    public View inflate(org.xmlpull.v1.XmlPullParser parser, ViewGroup root) { throw new UnsupportedOperationException("JET: XmlPullParser inflation"); }
    public View inflate(org.xmlpull.v1.XmlPullParser parser, ViewGroup root, boolean attach) { return inflate(parser, root); }
    public View inflate(int resource, ViewGroup root, boolean attach) {
        Element e = table().layout(resource);
        if (e.getTagName().equals("merge")) {
            if (root == null || !attach) throw new IllegalArgumentException("<merge /> can be used only with a valid ViewGroup root and attachToRoot=true");
            inflateChildren(e, root); return root;
        }
        View v = createViewFromTag(root, e);
        ViewGroup.LayoutParams lp = root != null ? root.generateLayoutParams(new XmlAttrs(e, table())) : null;
        if (lp != null && !attach) v.setLayoutParams(lp);
        if (v instanceof ViewGroup) inflateChildren(e, (ViewGroup) v);
        v.onFinishInflate();
        if (root != null && attach) { root.addView(v, lp); return root; }
        return v;
    }
    private void inflateChildren(Element parent, ViewGroup group) {
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (!(nl.item(i) instanceof Element)) continue;
            Element ce = (Element) nl.item(i);
            String tag = ce.getTagName();
            if (tag.equals("requestFocus") || tag.equals("tag")) continue;
            if (tag.equals("include")) { include(ce, group); continue; }
            View cv = createViewFromTag(group, ce);
            ViewGroup.LayoutParams lp = group.generateLayoutParams(new XmlAttrs(ce, table()));
            if (cv instanceof ViewGroup) inflateChildren(ce, (ViewGroup) cv);
            cv.onFinishInflate();
            group.addView(cv, lp);
        }
    }
    private void include(Element inc, ViewGroup group) {
        String l = inc.getAttribute("layout");
        int id = ResTable.refId(l);
        if (id == 0) throw new IllegalArgumentException("You must specify a valid layout reference in <include>: " + l);
        Element e = table().layout(id);
        if (e.getTagName().equals("merge")) { inflateChildren(e, group); return; }
        View v = createViewFromTag(group, e);
        if (v instanceof ViewGroup) inflateChildren(e, (ViewGroup) v);
        ViewGroup.LayoutParams ilp = group.generateLayoutParams(new XmlAttrs(e, table()));
        v.setLayoutParams(ilp);
        String iid = inc.getAttribute("android:id");
        if (iid != null && iid.startsWith("@")) v.setId(ResTable.refId(iid));
        XmlAttrs ia = new XmlAttrs(inc, table());
        if (ia.get("layout_width") != null && ia.get("layout_height") != null) {
            // AOSP LayoutInflater.parseInclude: <include> layout_* attrs (rules, margins, gravity...) override the included root's.
            try { v.setLayoutParams(group.generateLayoutParams(ia)); } catch (RuntimeException ignored) {}
        } else if (ia.get("layout_width") != null || ia.get("layout_height") != null) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            try { if (ia.get("layout_width") != null) lp.width = (int) table().dimen(ia.get("layout_width")); } catch (Exception ignored) {}
            try { if (ia.get("layout_height") != null) lp.height = (int) table().dimen(ia.get("layout_height")); } catch (Exception ignored) {}
        }
        String vis = inc.getAttribute("android:visibility");
        if ("gone".equals(vis) || "2".equals(vis)) v.setVisibility(View.GONE); else if ("invisible".equals(vis) || "1".equals(vis)) v.setVisibility(View.INVISIBLE);
        v.onFinishInflate();
        group.addView(v);
    }
    private View createViewFromTag(View parent, Element e) {
        String tag = e.getTagName();
        if (tag.equals("view")) tag = e.getAttribute("class");
        AttributeSet attrs = new XmlAttrs(e, table());
        View v = null;
        try {
            if (mFactory2 != null) v = mFactory2.onCreateView(parent, tag, mContext, attrs);
            else if (mFactory != null) v = mFactory.onCreateView(tag, mContext, attrs);
            if (v == null && mPrivateFactory != null) v = mPrivateFactory.onCreateView(parent, tag, mContext, attrs);
        } catch (RuntimeException re) {
            System.err.println("[jet] inflater factory failed for " + tag + ": " + re);
            v = null;
        }
        if (v != null) return v;
        return createView(tag, tag.indexOf('.') >= 0 ? null : (tag.equals("View") || tag.equals("ViewGroup") || tag.equals("ViewStub") || tag.equals("SurfaceView") || tag.equals("TextureView") ? "android.view." : "android.widget."), attrs);
    }
    public final View createView(String name, String prefix, AttributeSet attrs) {
        String cls = prefix != null && name.indexOf('.') < 0 ? prefix + name : name;
        try {
            ClassLoader cl = mContext.getClassLoader();
            if (cl == null) cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) cl = LayoutInflater.class.getClassLoader();
            Class<?> k = Class.forName(cls, true, cl);
            Constructor<?> ctor = k.getConstructor(Context.class, AttributeSet.class);
            return (View) ctor.newInstance(mContext, attrs);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable c = ite.getCause() != null ? ite.getCause() : ite;
            throw new InflateException("Binary XML: error inflating class " + cls, c);
        } catch (ReflectiveOperationException ex) {
            throw new InflateException("JET: unsupported view class " + cls + " (" + ex + ")", ex);
        }
    }
    public final View createView(Context c, String name, String prefix, AttributeSet attrs) { return createView(name, prefix, attrs); }
    protected View onCreateView(String name, AttributeSet attrs) { return createView(name, "android.view.", attrs); }
    protected View onCreateView(View parent, String name, AttributeSet attrs) { return onCreateView(name, attrs); }
    public View onCreateView(Context c, View parent, String name, AttributeSet attrs) { return onCreateView(parent, name, attrs); }
}
