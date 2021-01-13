package chat.rocket.rnshareextension

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class SharePackage : ReactPackage {

    override fun createNativeModules(reactContext: ReactApplicationContext) = listOf<NativeModule>(ShareModule(reactContext))

    override fun createViewManagers(reactContext: ReactApplicationContext) = emptyList<ViewManager<*, *>>()
}