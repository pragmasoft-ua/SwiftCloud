package swift.deployment

import java.util.concurrent.atomic.AtomicInteger

import static swift.deployment.Tools.*

class SwiftYCSB extends SwiftBase {
    static String YCSB_DRIVER = "swift.application.ycsb.SwiftMapPerKeyClient"
    static String INITDB_CMD = "-cp swiftcloud.jar -Djava.util.logging.config.file=logging.properties com.yahoo.ycsb.Client -db " + YCSB_DRIVER
    static String YCSB_CMD = "-Xincgc -cp swiftcloud.jar -Djava.util.logging.config.file=logging.properties com.yahoo.ycsb.Client -db " + YCSB_DRIVER

    static final WORKLOAD_A = ['recordcount':'1000',
        'operationcount':'1000',
        'workload':'com.yahoo.ycsb.workloads.CoreWorkload',
        'readallfields':'true',
        'readproportion':'0.5',
        'updateproportion':'0.5',
        'scanproportion':'0',
        'insertproportion':'0',
        'requestdistribution':'zipfian',
    ]

    static final WORKLOAD_B = ['recordcount':'1000',
        'operationcount':'1000',
        'workload':'com.yahoo.ycsb.workloads.CoreWorkload',
        'readallfields':'true',
        'readproportion':'0.95',
        'updateproportion':'0.05',
        'scanproportion':'0',
        'insertproportion':'0',
        'requestdistribution':'zipfian',
    ]

    private static HIGHLOCALITY = ['localrequestproportion' : '0.8']
    private static LOWLOCALITY = ['localrequestproportion' : '0.4']
    private static UNIFORM = ['requestdistribution': 'uniform']
    static WORKLOADS= [
        'workloada-uniform' : WORKLOAD_A + UNIFORM + HIGHLOCALITY,
        'workloada' : WORKLOAD_A + HIGHLOCALITY,
        'workloadb-uniform' : WORKLOAD_B + UNIFORM + HIGHLOCALITY,
        'workloadb' : WORKLOAD_B + HIGHLOCALITY,
        'workloada-uniform-lowlocality' : WORKLOAD_A + UNIFORM + LOWLOCALITY,
        'workloada-lowlocality' : WORKLOAD_A + LOWLOCALITY,
        'workloadb-uniform-lowlocality' : WORKLOAD_B + UNIFORM + LOWLOCALITY,
        'workloadb-lowlocality' : WORKLOAD_B + LOWLOCALITY,
    ]

    public static int initDB( String client, String server, String config, int threads = 1, String heap = "512m") {
        println "CLIENT: " + client + " SERVER: " + server + " CONFIG: " + config

        def cmd = INITDB_CMD + " -load -s -P " + config + " -p swift.hostname=" + server + " -threads " + threads +" "
        def res = rshC( client, swift_app_cmd_nostdout("-Xmx" + heap, cmd, "initdb-stderr.txt", "initdb-stdout.txt")).waitFor()
        println "OK.\n"
        return res
    }

    public static void runClients(List scoutGroups, String config, String shepard, int threads = 1, String heap ="512m" ) {
        def hosts = []

        scoutGroups.each{ hosts += it.all() }

        println hosts

        AtomicInteger n = new AtomicInteger();
        def resHandler = { host, res ->
            def str = n.incrementAndGet() + "/" + hosts.size() + (res < 1 ? " [ OK ]" : " [FAILED]") + " : " + host
            println str
        }

        scoutGroups.each { grp ->
            Thread.startDaemon {
                def cmd = { host ->
                    int index = hosts.indexOf( host );
                    // Use tail, because ofr for unknown reason "2> (tee scout-stderr 1>&2)" closes tee prematurely
                    def res = "nohup java " + SwiftBase.YOUR_KIT_PROFILER_JAVA_OPTION + "-Xmx" + heap + " " + YCSB_CMD + " -t -s -P " + config  + " -p swift.hostname=" + grp.dc.surrogates[index % grp.dc.surrogates.size()]
                    res += " -threads " + threads + " -shepard " + shepard + " 2> scout-stderr.txt > scout-stdout.txt  < /dev/null & sleep 1; tail -f scout-stderr.txt &"
                }
                grp.deploy( cmd, resHandler)
            }
        }
        // Parallel.rsh( clients, cmd, resHandler, true, 500000)
    }

    def opsNum = 10000000
    def incomingOpPerSecLimit = 12000

    def baseWorkload = WORKLOAD_A
    def localRecordCount = 150
    def ycsbProps
    def ycsbPropsPath
    def reportEveryOperation = true

    def initThreads = 2
    def initYcsbProps
    def initYcsbPropsPath

    public SwiftYCSB() {
        super()
    }

    protected void generateConfig() {
        def incomingOpPerSecPerClientLimit = (int) (incomingOpPerSecLimit / scouts.size())
        def workload = baseWorkload + ['recordcount': dbSize.toString(), 'operationcount':opsNum.toString(),
            'target':incomingOpPerSecPerClientLimit,

            'localpoolfromglobaldistribution':'true',
            'localrequestdistribution':'uniform',
            'localrecordcount': Integer.toString(localRecordCount),
        ]
        ycsbProps = DEFAULT_PROPS + workload + ['swift.reports' : reports.join(',')] + ['swift.reportEveryOperation':reportEveryOperation.toString()]
        ycsbProps += mode + ['maxexecutiontime' : duration]
        ycsbPropsPath = "swiftycsb.properties"
        initYcsbPropsPath = "swiftycsb-init.properties"

        // Options for DB initialization
        def initNoReports = ['swift.reports':'']
        def initOptions = SwiftBase.NO_CACHING_NOTIFICATIONS_PROPS
        initYcsbProps = SwiftYCSB.DEFAULT_PROPS + workload + ['target':'10000000'] + initNoReports + initOptions

        config = properties + ['incomingOpPerSecPerClientLimit' : incomingOpPerSecPerClientLimit, 'workload': workload,
            'ycsbProps': ycsbProps, 'initYcsbProps': initYcsbProps]
        println config
    }

    protected void deployConfig() {
        deployTo(allMachines, SwiftYCSB.genPropsFile(ycsbProps).absolutePath, ycsbPropsPath)
        deployTo(allMachines, SwiftYCSB.genPropsFile(initYcsbProps).absolutePath, initYcsbPropsPath)
    }

    protected void doInitDB() {
        def initDbDc = Topology.datacenters[0].surrogates[0]
        def initDbClient  = Topology.datacenters[0].sequencers[0]

        SwiftYCSB.initDB( initDbClient, initDbDc, initYcsbPropsPath, initThreads)
    }

    protected void doRunClients() {
        SwiftYCSB.runClients(Topology.scoutGroups, ycsbPropsPath, shepardAddr, threads, "3072m")
    }
}
