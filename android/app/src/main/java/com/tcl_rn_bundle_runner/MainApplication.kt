package com.tcl_rn_bundle_runner

import android.app.Application
import android.content.Context
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.config.ReactFeatureFlags
import com.facebook.soloader.SoLoader
import com.tcl_rn_bundle_runner.newarchitecture.MainApplicationReactNativeHost
import com.tcl_rn_bundle_runner.shims.ShimsPackage
import com.tcl_rn_bundle_runner.core.PanelRuntimeManager

class MainApplication : Application(), ReactApplication {

  private val mReactNativeHost: ReactNativeHost = object : ReactNativeHost(this) {
    override fun getUseDeveloperSupport(): Boolean = false

    override fun getPackages(): List<ReactPackage> {
      val packages = PackageList(this).packages
      // Packages that cannot be autolinked yet can be added manually here.
      packages.add(ShimsPackage())
      return packages
    }

    override fun getJSMainModuleName(): String = "index"

    override fun getJSBundleFile(): String? =
      PanelRuntimeManager.getJsBundlePath(this@MainApplication)
  }

  private val mNewArchitectureNativeHost: ReactNativeHost = MainApplicationReactNativeHost(this)

  override fun getReactNativeHost(): ReactNativeHost {
    return if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      mNewArchitectureNativeHost
    } else {
      mReactNativeHost
    }
  }

  override fun onCreate() {
    super.onCreate()
    // If you opted-in for the New Architecture, we enable the TurboModule system
    ReactFeatureFlags.useTurboModules = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
    SoLoader.init(this, /* native exopackage */ false)
    PanelRuntimeManager.refreshMainComponentFromManifest(this)
    initializeFlipper(this, reactNativeHost.reactInstanceManager)
  }

  companion object {
    @JvmStatic
    var MAIN_COMPONENT_NAME: String = ""

    /**
     * Loads Flipper in React Native templates. Call this in the onCreate method with something like
     * initializeFlipper(this, getReactNativeHost().getReactInstanceManager());
     */
    @JvmStatic
    private fun initializeFlipper(context: Context, reactInstanceManager: ReactInstanceManager) {
      if (BuildConfig.DEBUG) {
        try {
          /*
           We use reflection here to pick up the class that initializes Flipper,
          since Flipper library is not available in release mode
          */
          val aClass = Class.forName("com.tcl_rn_bundle_runner.ReactNativeFlipper")
          aClass
            .getMethod("initializeFlipper", Context::class.java, ReactInstanceManager::class.java)
            .invoke(null, context, reactInstanceManager)
        } catch (e: ClassNotFoundException) {
          e.printStackTrace()
        } catch (e: NoSuchMethodException) {
          e.printStackTrace()
        } catch (e: IllegalAccessException) {
          e.printStackTrace()
        } catch (e: java.lang.reflect.InvocationTargetException) {
          e.printStackTrace()
        }
      }
    }
  }
}
