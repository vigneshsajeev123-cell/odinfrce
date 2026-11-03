package com.memoryuniverse.universe.renderer

import android.content.Context
import android.view.SurfaceView
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.memoryuniverse.universe.camera.CameraState
import com.memoryuniverse.universe.math.Vector3D
import java.nio.ByteBuffer

/**
 * Filament Native 3D Engine Manager.
 * Configures Google's PBR Filament renderer for the Pitch-Black 3D Memory Universe.
 */
class FilamentUniverseEngine(private val context: Context) {

    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var view: View? = null
    private var camera: Camera? = null
    private var skybox: Skybox? = null
    private var indirectLight: IndirectLight? = null

    private var uiHelper: UiHelper? = null
    private var displayHelper: DisplayHelper? = null

    @Volatile
    var isInitialized: Boolean = false
        private set

    fun initialize(surfaceView: SurfaceView) {
        // Initialize Filament native runtime
        com.google.android.filament.Filament.init()

        val eng = Engine.create()
        engine = eng

        val rend = eng.createRenderer()
        renderer = rend

        val scn = eng.createScene()
        scene = scn

        val cam = eng.createCamera(EntityManager.get().create())
        camera = cam

        val vw = eng.createView()
        vw.scene = scn
        vw.camera = cam

        // Pitch-black universe skybox: pure black #000000
        val sky = Skybox.Builder()
            .color(0f, 0f, 0f, 1f)
            .build(eng)
        scn.skybox = sky
        skybox = sky

        // Ambient deep-space spherical harmonics light (subtle starlight)
        val shHarmonics = FloatArray(9 * 3) { 0.04f }
        val light = IndirectLight.Builder()
            .irradiance(3, shHarmonics)
            .intensity(8000f)
            .build(eng)
        scn.indirectLight = light
        indirectLight = light

        view = vw

        // Setup Display and UI Helper for swapchain management
        displayHelper = DisplayHelper(context)
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            renderCallback = object : UiHelper.RendererCallback {
                override fun onNativeWindowCreated(surface: android.view.Surface) {
                    val swapChain = eng.createSwapChain(surface)
                    // Bound to renderer
                }

                override fun onNativeWindowResized(surface: android.view.Surface, width: Int, height: Int) {
                    vw.viewport = Viewport(0, 0, width, height)
                }

                override fun onNativeWindowDestroyed(surface: android.view.Surface) {
                    // Cleanup swapchain
                }
            }
            attachTo(surfaceView)
        }

        isInitialized = true
    }

    fun updateCamera(cameraState: CameraState) {
        val cam = camera ?: return
        val eye = cameraState.getEffectiveEye()
        val target = cameraState.getEffectiveTarget()
        val up = cameraState.up

        cam.lookAt(
            eye.x.toDouble(), eye.y.toDouble(), eye.z.toDouble(),
            target.x.toDouble(), target.y.toDouble(), target.z.toDouble(),
            up.x.toDouble(), up.y.toDouble(), up.z.toDouble()
        )

        cam.setPerspective(
            cameraState.fovDegrees.toDouble(),
            cameraState.aspectRatio.toDouble(),
            0.1,
            3000.0,
            Camera.Fov.VERTICAL
        )
    }

    fun renderFrame() {
        val rend = renderer ?: return
        val vw = view ?: return
        // Render current frame
        if (rend.beginFrame(rend.engine.createSwapChain(null) ?: return)) {
            rend.render(vw)
            rend.endFrame()
        }
    }

    fun destroy() {
        isInitialized = false
        uiHelper?.detach()
        engine?.let { eng ->
            skybox?.let { eng.destroySkybox(it) }
            indirectLight?.let { eng.destroyIndirectLight(it) }
            view?.let { eng.destroyView(it) }
            camera?.let { eng.destroyCameraComponent(it.entity) }
            scene?.let { eng.destroyScene(it) }
            renderer?.let { eng.destroyRenderer(it) }
            eng.destroy()
        }
    }
}
