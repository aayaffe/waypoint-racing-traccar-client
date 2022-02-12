/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION", "StaticFieldLeak")
package `in`.avimarine.waypointracing.database

import `in`.avimarine.waypointracing.route.GatePassing
import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.AsyncTask
import java.sql.Date

class GatePassesDatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    interface DatabaseHandler<T> {
        fun onComplete(success: Boolean, result: T)
    }

    private abstract class DatabaseAsyncTask<T>(val handler: DatabaseHandler<T?>) : AsyncTask<Unit, Unit, T?>() {

        private var error: RuntimeException? = null

        override fun doInBackground(vararg params: Unit): T? {
            return try {
                executeMethod()
            } catch (error: RuntimeException) {
                this.error = error
                null
            }
        }

        protected abstract fun executeMethod(): T

        override fun onPostExecute(result: T?) {
            handler.onComplete(error == null, result)
        }
    }

    private val db: SQLiteDatabase = writableDatabase

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE gatepasses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "eventName TEXT, " +
                    "routeId TEXT, " +
                    "deviceId TEXT," +
                    "boatname TEXT," +
                    "gateId INTEGER," +
                    "gateName TEXT," +
                    "time INTEGER," +
                    "latitude REAL," +
                    "longitude REAL," +
                    "speed REAL," +
                    "course REAL," +
                    "accuracy REAL," +
                    "battery REAL," +
                    "mock INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS gatepasses;")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS gatepasses;")
        onCreate(db)
    }

    fun insertGatePass(gatePass: GatePassing) {
        val values = ContentValues()
        values.put("eventName", gatePass.eventName)
        values.put("routeId", gatePass.routeId)
        values.put("deviceId", gatePass.deviceId)
        values.put("boatname", gatePass.boatName)
        values.put("gateId", gatePass.gateId)
        values.put("gateName", gatePass.gateName)
        values.put("time", gatePass.time.time)
        values.put("latitude", gatePass.latitude)
        values.put("longitude", gatePass.longitude)
        values.put("speed", gatePass.speed)
        values.put("course", gatePass.course)
        values.put("accuracy", gatePass.accuracy)
        values.put("battery", gatePass.battery)
        values.put("mock", if (gatePass.mock) 1 else 0)
        db.insertOrThrow("gatepasses", null, values)
    }

    fun insertGatePassAsync(gatePass: GatePassing, handler: DatabaseHandler<Unit?>) {
        object : DatabaseAsyncTask<Unit>(handler) {
            override fun executeMethod() {
                insertGatePass(gatePass)
            }
        }.execute()
    }

    fun selectGatePass(): GatePassing? {
        db.rawQuery("SELECT * FROM gatepasses ORDER BY id LIMIT 1", null).use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()
                return GatePassing(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    eventName = cursor.getString(cursor.getColumnIndexOrThrow("eventName")),
                    routeId = cursor.getString(cursor.getColumnIndexOrThrow("routeId")),
                    deviceId = cursor.getString(cursor.getColumnIndexOrThrow("deviceId")),
                    boatName = cursor.getString(cursor.getColumnIndexOrThrow("boatname")),
                    gateId = cursor.getInt(cursor.getColumnIndexOrThrow("gateId")),
                    gateName = cursor.getString(cursor.getColumnIndexOrThrow("gateName")),
                    time = Date(cursor.getLong(cursor.getColumnIndexOrThrow("time"))),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    speed = cursor.getDouble(cursor.getColumnIndexOrThrow("speed")),
                    course = cursor.getDouble(cursor.getColumnIndexOrThrow("course")),
                    accuracy = cursor.getDouble(cursor.getColumnIndexOrThrow("accuracy")),
                    battery = cursor.getDouble(cursor.getColumnIndexOrThrow("battery")),
                    mock = cursor.getInt(cursor.getColumnIndexOrThrow("mock")) > 0,
                )
            }
        }
        return null
    }

    fun selectGatePassAsync(handler: DatabaseHandler<GatePassing?>) {
        object : DatabaseAsyncTask<GatePassing?>(handler) {
            override fun executeMethod(): GatePassing? {
                return selectGatePass()
            }
        }.execute()
    }

    fun deleteGatepass(id: Long) {
        if (db.delete("gatepasses", "id = ?", arrayOf(id.toString())) != 1) {
            throw SQLException()
        }
    }

    fun deleteGatePassAsync(id: Long, handler: DatabaseHandler<Unit?>) {
        object : DatabaseAsyncTask<Unit>(handler) {
            override fun executeMethod() {
                deleteGatepass(id)
            }
        }.execute()
    }

    companion object {
        const val DATABASE_VERSION = 3
        const val DATABASE_NAME = "traccar.gatepasses.db"
    }

}
