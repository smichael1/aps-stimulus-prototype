import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.Keys._
import sbt._
import com.typesafe.sbt.site.PreprocessSupport._

import Dependencies._
import Settings._

def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")

def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")

def runtime(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")

def container(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")


val vslice = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(UnidocRoot.settings(Nil, Nil): _*)
  .settings(defaultSettings: _*)
  .settings(siteSettings: _*)
  .settings(libraryDependencies ++=
    compile(akkaActor, akkaRemote, akkaHttp, pkg, cs, ccs, loc, ts, events, util, alarms, containerCmd, seqSupport) ++
      test(scalaTest, specs2, akkaTestKit)
   )
  .settings(packageSettings("VerticalSlice", "Vertical Slice Example", "More complicated example showing CSW features"): _*)
  


