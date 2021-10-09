#!bin/bash

optId=jjiijjhalakdngbgiahggnaldgh
jdkPath=/opt/java/jdk/jdk-11_0_10
abPath=$(cd `dirname $0`;pwd)	
jarName=vpn-server.jar
jarId=(`${jdkPath}/bin/jps | grep ${jarName}`)
jarId=${jarId[0]}
cronPath=/var/spool/cron/`whoami`
# cronPath=/etc/crontab
logFileName=console.log

# this line is required, because crontab path is not ${abPath}, so need cd
cd ${abPath}

help() {
	echo -----method---------
	echo start
	echo stop
	echo restart
	echo auto_reboot
	echo cron_start
	echo cron_stop
}

vpnlog() {
	logPath=logs
	now=`date "+%Y-%m-%d %H:%M:%S"`

	if [ ! -d ${logPath} ]; then
		mkdir -p ${logPath}
		echo create folder ${logPath}
	fi

	if [ -f ${logFileName} ]; then
		# mv ${logFileName} ${logPath}/${now}.log
		echo aa
	fi
	echo ${now} ${1} >> ${logFileName}
}

start() {
	vpnlog "exec ${jdkPath}/bin/java ${jarName}"
	${jdkPath}/bin/java -jar ${jarName} -Xms512M -Xmx512M -XX:+HeapDumpOnOutOfMemoryError -Xlogg${abPath}/logs/gc-%t.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=20M -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCCause 2 >& 1 >> ${logFileName} &
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
	# appInfo=(`top -p ${jarId} -n 1 | grep ${jarId}`)
	# tip this place in centos8, the first index is 1

	memSizeKb=(`cat /proc/${jarId}/status | grep VmRSS`)
	# number compare (=:-eq !=:-ne >:-gt >=:-ge <:-lt <=:-le)
	if [ ${memSizeKb[1]} -gt 300000 ]; then
		restart
	fi
}

cron_start() {
	# * \x2A encode
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

	# delete rule by string
	sed -i "/${optId}/d" ${cronPath}
	echo delete ${optId} line in ${cronPath}
}


${1}
