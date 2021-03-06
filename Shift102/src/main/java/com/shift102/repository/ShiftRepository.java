package com.shift102.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import com.shift102.model.ExceptionData;
import com.shift102.model.Shift;
import com.shift102.model.UserShiftData;
import com.shift102.util.ShiftMapper;

@Repository
public class ShiftRepository extends RepositoryCommon{

	
	@Autowired
	private ShiftMapper mapper;
	private final int INSERT_BATCH_SIZE = 5;
	private final int UPDATE_BATCH_SIZE = 5;
	private final String insertQuery="INSERT INTO SHIFTDATA(DATA_CALENDAR_ID, DATA_SHIFT_ID, DATA_EXCEPTION_ID, "
			+ "DATA_USER_ID, DATA_DATES, DATA_LASTUPDATED, DATA_LASTUPDATEDBY) VALUES(?,?,?,?,?,?,?)";
	private final String updateQuery="UPDATE SHIFTDATA set DATA_CALENDAR_ID=?, DATA_SHIFT_ID=?, DATA_EXCEPTION_ID=?, "
			+ "DATA_USER_ID=?, DATA_DATES=?, DATA_LASTUPDATED=?, DATA_LASTUPDATEDBY=? where DATA_ID=?";

	public Shift getShiftByMonthYear() {

		return this.getShiftByMonthYear(null, null);
	}

	public Shift getShiftByMonthYear(String month, String year) {
		Shift shift=null;
		if (month == null || year == null) {
			java.util.Date date = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			month = String.valueOf(cal.get(Calendar.MONTH) + 1);
			year = String.valueOf(cal.get(Calendar.YEAR));
		}
		SqlRowSet srs;
		try {
			srs = jdbcTemplate
					.queryForRowSet("SELECT DATA_ID,CAL_MONTH,CAL_YEAR,SHIFT_ID,SHIFT_NAME,SHIFT_START,SHIFT_END,SHIFT_TIMEZONE, "
							+ "USER_ID,USER_NAME,EXCP_ID,EXCP_EXCEPTIONNAME, " + "DATA_DATES " + "FROM SHIFTDATA "
							+ "JOIN CALENDAR ON CAL_ID=DATA_CALENDAR_ID " + "JOIN SHIFTS ON SHIFT_ID=DATA_SHIFT_ID "
							+ "JOIN EXCEPTIONTYPES ON EXCP_ID=DATA_EXCEPTION_ID " + "JOIN USERDATA ON USER_ID=DATA_USER_ID "
							+ "WHERE CAL_MONTH= ? AND CAL_YEAR=?",month,year);
			
			shift=mapper.mapShift(srs,Integer.valueOf(month),Integer.valueOf(year));
		}catch(Exception e) {
			e.printStackTrace();
		}
		return shift;
	}
	
	public void shiftUpdate(Shift shift) {
		HashMap<String,Object> insertMap,updateMap ;
		List<Integer> usersUpdated = new ArrayList<Integer>();
		List<HashMap<String,Object>> insertList = new ArrayList<HashMap<String,Object>>();
		List<HashMap<String,Object>> updateList = new ArrayList<HashMap<String,Object>>();
		for(UserShiftData userShift:shift.getUsershift()) {
			if(usersUpdated.contains(userShift.getUser_id()))
				continue;
			usersUpdated.add(userShift.getUser_id());
			for(ExceptionData exception:userShift.getExceptionData()) {
				if(exception.getData_Id() == null) {
					//Insert a new record
					insertMap = new HashMap<String,Object>();
					insertMap.put("DATA_CALENDAR_ID", shift.getCalendar_id());
					insertMap.put("DATA_SHIFT_ID", userShift.getShift_id());
					insertMap.put("DATA_EXCEPTION_ID", exception.getExcp_id());
					insertMap.put("DATA_USER_ID", userShift.getUser_id());
					insertMap.put("DATA_DATES", Arrays.toString(exception.getDates()).replace("[", "").replace("]", ""));
					insertMap.put("DATA_LASTUPDATED", new java.sql.Timestamp(new java.util.Date().getTime()));
					insertMap.put("DATA_LASTUPDATEDBY", "ADMIN");
					insertList.add(insertMap);
					//insertShiftData(shift.getCalendar_id(),userShift,exception);
				}else {
					//update the existing record
					updateMap = new HashMap<String,Object>();
					updateMap.put("DATA_CALENDAR_ID", shift.getCalendar_id());
					updateMap.put("DATA_SHIFT_ID", userShift.getShift_id());
					updateMap.put("DATA_EXCEPTION_ID", exception.getExcp_id());
					updateMap.put("DATA_USER_ID", userShift.getUser_id());
					updateMap.put("DATA_DATES", Arrays.toString(exception.getDates()).replace("[", "").replace("]", ""));
					updateMap.put("DATA_LASTUPDATED", new java.sql.Timestamp(new java.util.Date().getTime()));
					updateMap.put("DATA_LASTUPDATEDBY", "ADMIN");
					updateMap.put("DATA_ID",exception.getData_Id());
					updateList.add(updateMap);
				}
			}
		}
		if(insertList.size()>0)insert(insertList);
		if(updateList.size()>0)update(updateList);

    }
	
	public void insert(List<HashMap<String,Object>> shiftData) {
		for (int i = 0; i < shiftData.size(); i += INSERT_BATCH_SIZE) {
			final List<HashMap<String,Object>> batchList = shiftData.subList(i, i
					+ INSERT_BATCH_SIZE > shiftData.size() ? shiftData.size() : i
					+ INSERT_BATCH_SIZE);
			jdbcTemplate.batchUpdate(insertQuery,
					new BatchPreparedStatementSetter() {
						@Override
						public void setValues(PreparedStatement pStmt, int j)
								throws SQLException {
							Map<String,Object> map = shiftData.get(j);

							pStmt.setInt(1, Integer.parseInt(map.get("DATA_CALENDAR_ID").toString()));
							pStmt.setInt(2, Integer.parseInt(map.get("DATA_SHIFT_ID").toString()));
							pStmt.setInt(3, Integer.parseInt(map.get("DATA_EXCEPTION_ID").toString()));
							pStmt.setInt(4, Integer.parseInt(map.get("DATA_USER_ID").toString()));
							pStmt.setString(5, map.get("DATA_DATES").toString());
							pStmt.setTimestamp(6, (java.sql.Timestamp)map.get("DATA_LASTUPDATED"));
							pStmt.setString(7, (String)map.get("DATA_LASTUPDATEDBY"));
						}
						@Override
						public int getBatchSize() {
							return batchList.size();
						}
					});
		}
	}
	
	public void update(List<HashMap<String,Object>> shiftData) {
		for (int i = 0; i < shiftData.size(); i += UPDATE_BATCH_SIZE) {
			final List<HashMap<String,Object>> batchList = shiftData.subList(i, i
					+ UPDATE_BATCH_SIZE > shiftData.size() ? shiftData.size() : i
					+ UPDATE_BATCH_SIZE);
			jdbcTemplate.batchUpdate(updateQuery,
					new BatchPreparedStatementSetter() {
						@Override
						public void setValues(PreparedStatement pStmt, int j)
								throws SQLException {
							Map<String,Object> map = shiftData.get(j);

							pStmt.setInt(1, Integer.parseInt(map.get("DATA_CALENDAR_ID").toString()));
							pStmt.setInt(2, Integer.parseInt(map.get("DATA_SHIFT_ID").toString()));
							pStmt.setInt(3, Integer.parseInt(map.get("DATA_EXCEPTION_ID").toString()));
							pStmt.setInt(4, Integer.parseInt(map.get("DATA_USER_ID").toString()));
							pStmt.setString(5, map.get("DATA_DATES").toString());
							pStmt.setTimestamp(6, (java.sql.Timestamp)map.get("DATA_LASTUPDATED"));
							pStmt.setString(7, (String)map.get("DATA_LASTUPDATEDBY"));
							pStmt.setInt(8, Integer.parseInt(map.get("DATA_ID").toString()));
						}
						@Override
						public int getBatchSize() {
							return batchList.size();
						}
					});
		}
	}
	
	/*
	 * private HashMap<String, Object> getDateMap(String cal_id,UserShiftData
	 * userShift,ExceptionData exception) { return getDateMap(cal_id,userShift,
	 * exception,null); }
	 * 
	 * private HashMap<String, Object> getDateMap(String cal_id,UserShiftData
	 * userShift,ExceptionData exception,String data_id) { HashMap<String,Object>
	 * map =new HashMap<String,Object>(); map.put("data_id", data_id);
	 * map.put("cal_id", cal_id); map.put("shift_id", userShift.getShift_id());
	 * map.put("excp_id",exception.getExcp_id()); map.put("user_id",
	 * userShift.getUser_id()); map.put("dates",
	 * Arrays.toString(exception.getDates()).replace("[", "").replace("]", ""));
	 * map.put("last_upd", "current_timestamp"); map.put("last_upd_by", "ADMIN");
	 * 
	 * return map;
	 * 
	 * }
	 */

	private void insertShiftData(String cal_id,UserShiftData userShift,ExceptionData exception) {
		Map<String,Object> inputMap = new HashMap<String,Object>();
		inputMap.put("DATA_CALENDAR_ID", cal_id);
		inputMap.put("DATA_SHIFT_ID", userShift.getShift_id());
		inputMap.put("DATA_EXCEPTION_ID", exception.getExcp_id());
		inputMap.put("DATA_USER_ID", userShift.getUser_id());
		inputMap.put("DATA_DATES", Arrays.toString(exception.getDates()).replace("[", "").replace("]", ""));
		inputMap.put("DATA_LASTUPDATED", new java.sql.Timestamp(new java.util.Date().getTime()));
		inputMap.put("DATA_LASTUPDATEDBY", "ADMIN");
		
		
		SimpleJdbcInsert insertDao = new SimpleJdbcInsert(jdbcTemplate);
		insertDao.setTableName("SHIFTDATA");
		insertDao.setGeneratedKeyName("DATA_ID");
		insertDao.executeAndReturnKey(inputMap).intValue();

		
	}

	public List<Integer> getYears() {
		SqlRowSet srs = jdbcTemplate.queryForRowSet("SELECT DISTINCT CAL_YEAR FROM CALENDAR ORDER BY CAL_YEAR");
		List<Integer> years = new ArrayList<Integer>();
		int year = 0;
		while (srs.next()) {
			year = srs.getInt("CAL_YEAR");
			years.add(year);

		}
		if (!years.contains(Calendar.getInstance().get(Calendar.YEAR) - 1)) {
			years.add(Calendar.getInstance().get(Calendar.YEAR) - 1);
		}
		if (!years.contains(Calendar.getInstance().get(Calendar.YEAR) + 1)) {
			years.add(Calendar.getInstance().get(Calendar.YEAR) + 1);
		}
		if (!years.contains(Calendar.getInstance().get(Calendar.YEAR))) {
			years.add(Calendar.getInstance().get(Calendar.YEAR));
		}
		Collections.sort(years);
		return years;
	}



	public int insertCalendarItem(Map<String, Object> inputMap) {

		SimpleJdbcInsert insertDao = new SimpleJdbcInsert(jdbcTemplate);
		insertDao.setTableName("CALENDAR");
		insertDao.setGeneratedKeyName("CAL_ID");
		int id = insertDao.executeAndReturnKey(inputMap).intValue();

		return id;
	}

	public void setCalendarId(Shift shift) {
		SqlRowSet srs = jdbcTemplate.queryForRowSet("SELECT CAL_ID FROM CALENDAR WHERE CAL_MONTH=" + shift.getMonth_id()
				+ " AND CAL_YEAR=" + shift.getYear());

		while (srs.next()) {
			shift.setCalendar_id(String.valueOf(srs.getInt("CAL_ID")));
		}

	}

	public void updateShifts(Shift shift) {

	}

}
