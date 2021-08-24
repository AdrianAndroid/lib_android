/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.processor

import androidx.room.FtsOptions
import androidx.room.parser.FtsVersion
import androidx.room.parser.SQLTypeAffinity
import androidx.room.vo.CallType
import androidx.room.vo.Field
import androidx.room.vo.FieldGetter
import androidx.room.vo.FieldSetter
import androidx.room.vo.Fields
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class Fts3TableEntityProcessorTest : BaseFtsEntityParserTest() {

    override fun getFtsVersion() = 3

    @Test
    fun simple() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                private int rowId;
                public int getRowId() { return rowId; }
                public void setRowId(int id) { this.rowId = rowId; }
            """
        ) { entity, invocation ->
            assertThat(entity.type.typeName.toString(), `is`("foo.bar.MyEntity"))
            assertThat(entity.fields.size, `is`(1))
            val field = entity.fields.first()
            val intType = invocation.processingEnv.requireType(TypeName.INT)
            assertThat(
                field,
                `is`(
                    Field(
                        element = field.element,
                        name = "rowId",
                        type = intType,
                        columnName = "rowid",
                        affinity = SQLTypeAffinity.INTEGER
                    )
                )
            )
            assertThat(field.setter, `is`(FieldSetter("setRowId", intType, CallType.METHOD)))
            assertThat(field.getter, `is`(FieldGetter("getRowId", intType, CallType.METHOD)))
            assertThat(entity.primaryKey.fields, `is`(Fields(field)))
            assertThat(entity.shadowTableName, `is`("MyEntity_content"))
            assertThat(entity.ftsVersion, `is`(FtsVersion.FTS3))
        }
    }

    @Test
    fun omittedRowId() {
        singleEntity(
            """
                private String content;
                public String getContent() { return content; }
                public void setContent(String content) { this.content = content; }
            """
        ) { _, _ -> }
    }

    @Test
    fun missingPrimaryKeyAnnotation() {
        singleEntity(
            """
                @ColumnInfo(name = "rowid")
                private int rowId;
                public int getRowId(){ return rowId; }
                public void setRowId(int rowId) { this.rowId = rowId; }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.MISSING_PRIMARY_KEYS_ANNOTATION_IN_ROW_ID
                )
            }
        }
    }

    @Test
    fun badPrimaryKeyName() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public int getId(){ return id; }
                public void setId(int id) { this.id = id; }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_NAME
                )
            }
        }
    }

    @Test
    fun badPrimaryKeyAffinity() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                private String rowId;
                public String getRowId(){ return rowId; }
                public void setRowId(String rowId) { this.rowId = rowId; }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_AFFINITY
                )
            }
        }
    }

    @Test
    fun multiplePrimaryKeys() {
        singleEntity(
            """
                @PrimaryKey
                public int oneId;
                @PrimaryKey
                public int twoId;
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.TOO_MANY_PRIMARY_KEYS_IN_FTS_ENTITY
                )
            }
        }
    }

    @Test
    fun nonDefaultTokenizer() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                private int rowId;
                public int getRowId() { return rowId; }
                public void setRowId(int id) { this.rowId = rowId; }
                """,
            ftsAttributes = hashMapOf("tokenizer" to "FtsOptions.TOKENIZER_PORTER")
        ) { entity, _ ->
            assertThat(entity.ftsOptions.tokenizer, `is`(FtsOptions.TOKENIZER_PORTER))
        }
    }

    @Test
    fun customTokenizer() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                private int rowId;
                public int getRowId() { return rowId; }
                public void setRowId(int id) { this.rowId = rowId; }
                """,
            ftsAttributes = hashMapOf("tokenizer" to "\"customICU\"")
        ) { entity, _ ->
            assertThat(entity.ftsOptions.tokenizer, `is`("customICU"))
        }
    }
}