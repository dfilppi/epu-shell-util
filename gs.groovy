import org.openspaces.admin.AdminFactory
import org.openspaces.admin.space.*
import org.openspaces.core.util.MemoryUnit
import org.openspaces.admin.pu.elastic.config.*
import org.openspaces.admin.pu.elastic.*
import java.util.concurrent.TimeUnit

// Acts as a decorator for gs.sh in order to add new functionality
// Returns 0 if no action taken, 99 if command was consumed

def cli
def used=false

if(args.length>0){

  switch(args[0]){
    //Deploy Elastic Stateful PU
    case "deploy-stateful-epu":
      cli=new CliBuilder(usage:'deploy-stateful-epu -f <pu-path> -s <container-size> -m <max-size> [options...]')
      cli.s(longOpt:'container-size',args:1,argName:'csize',required:true,'Container size')
      cli.m(longOpt:'max-capacity',args:1,argName:'maxc','Largest possible total grid size')
      cli.i(longOpt:'initial-capacity',args:1,argName:'initial','Initial total grid size')
      cli.n(longOpt:'pu-name',args:1,argName:'name','Processing Unit name')
      cli.o(longOpt:'single-machine',argName:'single','Single machine deployment')
      cli.c(longOpt:'max-cores',args:1,argName:'cores','Max # cpu cores')
      cli.k(longOpt:'initial-cores',args:1,argName:'icores','Initial # cpu cores')
      cli.e(longOpt:'eager-mode',argName:'eager','Eager scale mode')
      cli.p(longOpt:'num-partitions',args:1,argName:'partitions','Number of partitions')
      cli.b(longOpt:'num-backups',args:1,argName:'backups','Number of backups per partition')
      cli.z(longOpt:'zones',args:1,argName:'zones',valueSeparator:',','Zones')
      cli.r(longOpt:'reserved-mem',args:1, argName:'resmem', 'Reseved memory per machine')

      cli.f(longOpt:'pu-file',args:1,argName:'filename',required:true,'Processing Unit file name')

      def options=cli.parse(args[1..args.length-1])

      if(options==null)System.exit(1)

      dp=new ElasticStatefulProcessingUnitDeployment(new File(options.f))

      setCommonOptions(dp,options)

		deploy(dp)

      used=true

      break
 
    //Deploy Elastic Space
    case "deploy-espace":
      cli=new CliBuilder(usage:'deploy-espace -n <space-name> -s <container-size> -m <max-size> [options...]')
      cli.s(longOpt:'container-size',args:1,argName:'csize',required:true,'Container size')
      cli.m(longOpt:'max-capacity',args:1,argName:'maxc','Largest possible total grid size')
      cli.i(longOpt:'initial-capacity',args:1,argName:'initial','Initial total grid size')
      cli.o(longOpt:'single-machine',argName:'single','Single machine deployment')
      cli.c(longOpt:'max-cores',args:1,argName:'cores','Max # cpu cores')
      cli.k(longOpt:'initial-cores',args:1,argName:'icores','Initial # cpu cores')
      cli.e(longOpt:'eager-mode',argName:'eager','Eager scale mode')
      cli.p(longOpt:'num-partitions',args:1,argName:'partitions','Number of partitions')
      cli.b(longOpt:'num-backups',args:1,argName:'backups','Number of backups per partition')
      cli.z(longOpt:'zones',args:1,argName:'zones',valueSeparator:',','Zones')
      cli.r(longOpt:'reserved-mem',args:1, argName:'resmem', 'Reseved memory per machine')

      cli.n(longOpt:'name',args:1,argName:'name',required:true,'Space name')

      if(args.length==1){ cli.usage(); System.exit(1);}
      def options=cli.parse(args[1..args.length-1])

      if(options==null)System.exit(1)

      def dp=new ElasticSpaceDeployment(options.name)
      setCommonOptions(dp,options)
      deploy(dp)

      used=true
 
      break

    //Deploy Elastic Stateless PU
    case "deploy-stateless-epu":
      cli=new CliBuilder(usage:'deploy-stateless-epu -f <file-name> -s <container-size> -c <num-cores>')
      cli.f(longOpt:'pu-file',args:1,argName:'filename',required:true,'Processing Unit file name')
      cli.s(longOpt:'container-size',args:1,argName:'csize',required:true,'Container size')
      cli.c(longOpt:'num-cores',args:1,argName:'cores',requred:true,'# cpu cores')
      cli.n(longOpt:'pu-name',args:1,argName:'name','Processing Unit name')

      if(args.length==1){ cli.usage(); System.exit(1);}
      def options=cli.parse(args[1..args.length-1])
      if(options==null)System.exit(1)

		def dp=new ElasticStatelessProcessingUnitDeployment(options.f)\
			.memoryCapacityPerContainer(normalizeSize(options.s),MemoryUnit.MEGABYTES)\
			.scale(\
				new ManualCapacityScaleConfigurer().numberOfCpuCores(options.c).create())
      if(options.n)dp.name(options.n)

      deploy(dp)

      used=true
      break

   //Scale Epu
   case "scale-epu":
      cli=new CliBuilder(usage:'scale-epu -m <memory-capacity> -c <cores>')
      cli.n(longOpt:'epu-name',args:1,argName:'epu-name',required:true,'Processing Unit name')
      cli.c(longOpt:'num-cores',args:1,argName:'cores','# cpu cores')
      cli.m(longOpt:'mem-capacity',args:1,argName:'mem-capacity','Memory capacity')

      if(args.length==1){ cli.usage(); System.exit(1);}
      def options=cli.parse(args[1..args.length-1])
      if(options==null)System.exit(1)

		def ms=new ManualCapacityScaleConfigurer()
      if(options.m)ms.memoryCapacity(normalizeSize(options.m),MemoryUnit.MEGABYTES)
      if(options.c)ms.numberOfCpuCores(Integer.parseInt(options.c))

		def admin=new AdminFactory().useDaemonThreads(true).create()
		def pu=admin.getProcessingUnits().waitFor(options.n,5,TimeUnit.SECONDS)
      if(pu==null)System.exit(1)
      pu.scale(ms.create())
      used=true
		break
  }

}

if(used)System.exit(99)

System.exit 0


///////////////////////////////
// UTILITY METHODS
///////////////////////////////

def deploy(dp){
  //note: assumes locators or group set in environment
  def admin=new AdminFactory().useDaemonThreads(true).create()
  admin.getGridServiceAgents().waitForAtLeastOne()
  admin.getElasticServiceManagers().waitForAtLeastOne()
  def gsm=admin.getGridServiceManagers().waitForAtLeastOne()

  gsm.deploy(dp)
}

// Normalize size string to number denominated in MB
def normalizeSize(sizestring){
   if(sizestring.length()<=0)throw "invalid size"
   def lc=sizestring.substring(sizestring.length()-1)
   lc=lc.toLowerCase()
   base=Integer.parseInt(sizestring.substring(0,sizestring.length()-1))
	if(lc!='g' && lc!='m')return Integer.parseInt(sizestring)  //default is mb
   if(lc=='g')base*=1024
	return base
}


def setCommonOptions(dp,options){

  dp.memoryCapacityPerContainer(normalizeSize(options.s),MemoryUnit.MEGABYTES)

  if(options.m){
     dp.maxMemoryCapacity(normalizeSize(options.m),MemoryUnit.MEGABYTES)
  }

  if(options.c){
    dp.maxNumberOfCpuCores(Integer.parseInt(options.c))
  }

  def dc=new DiscoveredMachineProvisioningConfigurer()
  if(options.z){
    z.each{
      dc.addGridServiceAgentZone(it)
    }
    dc.removeGridServiceAgentsWithoutZone()
  }
  if(options.r){
    dc.reservedMemoryCapacityPerMachine(normalizeSize(options.r),MemoryUnit.MEGABYTES)
  }
  dp.dedicatedMachineProvisioning(dc.create())

  if(options.o)dp.singleMachineDeployment();
  if(options.p)dp.numberOfPartitions(Integer.parseInt(options.p))
  if(options.b)dp.numberOfBackupsPerPartition(Integer.parseInt(options.b))
  if(options.n)dp.name(options.n)

  def scaler
  if(options.e){
    scaler=new EagerScaleConfig()
    dp.scale(scaler)
  }
  else{
   scaler=new ManualCapacityScaleConfigurer()
   scaler.memoryCapacity(normalizeSize(options.i),MemoryUnit.MEGABYTES)
   if(options.k)scaler.numberOfCpuCores(Integer.parseInt(options.k))
   dp.scale(scaler.create())
  }

}
