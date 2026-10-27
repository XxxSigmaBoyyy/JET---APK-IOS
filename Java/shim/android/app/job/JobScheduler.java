package android.app.job;
import java.util.*;
/** Records scheduled jobs; the headless host never runs them (no system job service). */
public abstract class JobScheduler {
    public static final int RESULT_FAILURE = 0, RESULT_SUCCESS = 1;
    public abstract int schedule(JobInfo job);
    public abstract int enqueue(JobInfo job, Object work);
    public abstract void cancel(int jobId);
    public abstract void cancelAll();
    public abstract List<JobInfo> getAllPendingJobs();
    public abstract JobInfo getPendingJob(int jobId);
    // API 34 namespaces: each namespace is its own job set
    private final Map<String, JobScheduler> jetNs = new HashMap<>();
    public synchronized JobScheduler forNamespace(String ns) { if (ns == null) throw new NullPointerException("namespace"); return jetNs.computeIfAbsent(ns, k -> new Jet(k)); }
    public String getNamespace() { return null; }
    public Map<String, List<JobInfo>> getPendingJobsInAllNamespaces() { Map<String, List<JobInfo>> r = new HashMap<>(); r.put(null, getAllPendingJobs()); for (Map.Entry<String, JobScheduler> e : jetNs.entrySet()) r.put(e.getKey(), e.getValue().getAllPendingJobs()); return r; }
    public void cancelInAllNamespaces() { cancelAll(); for (JobScheduler j : jetNs.values()) j.cancelAll(); }
    public int getPendingJobReason(int jobId) { return 0; }
    public boolean canRunUserInitiatedJobs() { return true; }
    public static final class Jet extends JobScheduler {
        private final Map<Integer, JobInfo> jobs = new LinkedHashMap<>(); private final String ns;
        public Jet() { this(null); } Jet(String ns) { this.ns = ns; }
        @Override public String getNamespace() { return ns; }
        public synchronized int schedule(JobInfo j) { jobs.put(j.getId(), j); return RESULT_SUCCESS; }
        public synchronized int enqueue(JobInfo j, Object w) { return schedule(j); }
        public synchronized void cancel(int id) { jobs.remove(id); }
        public synchronized void cancelAll() { jobs.clear(); }
        public synchronized List<JobInfo> getAllPendingJobs() { return new ArrayList<>(jobs.values()); }
        public synchronized JobInfo getPendingJob(int id) { return jobs.get(id); }
    }
}
