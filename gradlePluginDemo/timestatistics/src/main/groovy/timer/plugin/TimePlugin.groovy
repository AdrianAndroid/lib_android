
package joyy

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public class TimePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println ("TimePlugin load sucess")
        AppExtension appExtension = project.getExtensions().getByType(AppExtension.class)
        appExtension.registerTransform(new TimePluginTransform())

    }
}