#!bin/bash

optId=jjiijjhalakdngbgiahggnaldgh
jdkPath=/opt/java/jdk/jdk-11_0_10
abPath=$(cd `dirname $0`;pwd)	
jarName=vpn-server.jar
jarId=(`${jdkPath}/bin/jps | grep ${jarName}`)
jarId=${jarId[0]}
now=`date +%Y%m%d%H%M%S`
cronPath=/var/spool/cron/`whoami`
logPath=logs
logFileName=console.log

if [ ! -d ${logPath} ]; then
	mkdir -p ${logPath}
	echo create folder ${logPath}
fi

start() {
	if [ -f ${logFileName} ];then
		mv ${logFileName} ${logPath}/${now}.log
		echo backup log to ${logPath}/${now}.log
	fi
	echo start ${abPath}/vpn-server.jar
	$jdkPath/bin/java -jar ${abPath}/${jarName} -Xms512M -Xmx512M -XX:+HeapDumpOnOutOfMemoryError -Xlogg${abPath}/logs/gc-%t.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=20M -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCCause  1 >& ${logFileName}  &
}

stop() {
	kill ${jarId}
}

restart() {
	stop
	sleep 1
	start
}

# reboot shell
auto_reboot() {
	echo ${now} auto_reboot start >> ${abPath}/${logFileName} 
	
	# appInfo=(`top -p ${jarId} -n 1 | grep ${jarId}`)
	# 注意 这里在centos8中 第一个下标=1
	
	memSizeKb=(`cat /proc/${jarId}/status | grep VmRSS`)
	# number compare (=:-eq !=:-ne >:-gt >=:-ge <:-lt <=:-le)
	if [ ${memSizeKb[1]} -gt 300000 ]; then
		echo ${now} auto_reboot run >> ${abPath}/${logFileName}
		restart	
	fi
}

cron_start1() {
	# if no rule
	if [ ! -f ${cronPath} ];then
		add_scheduler_rule
	elif [ `grep -c "${optId}" ${cronPath}` -eq "0" ]; then
		add_scheduler_rule
	fi
}

cron_start() {
	schContent="\x2A/10 \x2A \x2A \x2A \x2A sh ${abPath}/${0} auto_reboot # ${optId}"
	# add rule
	echo -e ${schContent} >> ${cronPath}
	# console 
	echo -e ------ add cron rules: ${schContent}	
}

cron_stop() {
	# find rule line number
	# cronLineNum=`cat ${cronPath} | grep -n "${optId}" | cut -d: -f1`
	# delete rule by line number
	# sed -i '${cronLineNum}d' ${cronPath}
	# console
	# echo delete ${cronLineNum} line in ${cronPath}
	
	# delete rule by string
	sed -i "/${optId}/d" ${cronPath}
	echo delete ${optId} line in ${cronPath}
}

${1}
