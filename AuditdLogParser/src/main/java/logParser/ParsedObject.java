package logParser;

import java.util.Date;

public class ParsedObject {
	private String procName, objName, nameType, callType, inodeVal, procActName, procActLibDB, procActStartTime,
			procActCreatedDB, callKey, objPath, pid, lineToProcess, createdObjPath, uid, syscall;

	private Date syscallDate;
	private int exitVal, a0;

	private boolean isLib;

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getProcName() {
		return procName;
	}

	public void setProcName(String procName) {
		this.procName = procName;
	}

	public String getObjName() {
		return objName;
	}

	public void setObjName(String objName) {
		this.objName = objName;
	}

	public String getNameType() {
		return nameType;
	}

	public void setNameType(String nameType) {
		this.nameType = nameType;
	}

	public String getCallType() {
		return callType;
	}

	public void setCallType(String callType) {
		this.callType = callType;
	}

	public String getInodeVal() {
		return inodeVal;
	}

	public void setInodeVal(String inodeVal) {
		this.inodeVal = inodeVal;
	}

	public String getProcActName() {
		return procActName;
	}

	public void setProcActName(String procActName) {
		this.procActName = procActName;
	}

	public String getProcActLibDB() {
		return procActLibDB;
	}

	public void setProcActLibDB(String procActLibDB) {
		this.procActLibDB = procActLibDB;
	}

	public String getProcActStartTime() {
		return procActStartTime;
	}

	public void setProcActStartTime(String procActStartTime) {
		this.procActStartTime = procActStartTime;
	}

	public String getProcActCreatedDB() {
		return procActCreatedDB;
	}

	public void setProcActCreatedDB(String procActCreatedDB) {
		this.procActCreatedDB = procActCreatedDB;
	}

	public String getCallKey() {
		return callKey;
	}

	public void setCallKey(String callKey) {
		this.callKey = callKey;
	}

	public String getObjPath() {
		return objPath;
	}

	public void setObjPath(String objPath) {
		this.objPath = objPath;
	}

	public Date getSyscallDate() {
		return syscallDate;
	}

	public void setSyscallDate(Date date) {
		this.syscallDate = date;
	}

	public String getLineToProcess() {
		return lineToProcess;
	}

	public void setLineToProcess(String lineToProcess) {
		this.lineToProcess = lineToProcess;
	}

	public int getExitVal() {
		return exitVal;
	}

	public void setExitVal(int exitVal) {
		this.exitVal = exitVal;
	}

	public int getA0() {
		return a0;
	}

	public void setA0(int a0) {
		this.a0 = a0;
	}

	public String getCreatedObjPath() {
		return createdObjPath;
	}

	public void setCreatedObjPath(String createdObjPath) {
		this.createdObjPath = createdObjPath;
	}

	public boolean isLib() {
		return isLib;
	}

	public void setLib(boolean isLib) {
		this.isLib = isLib;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getSyscall() {
		return syscall;
	}

	public void setSyscall(String syscall) {
		this.syscall = syscall;
	}
}
