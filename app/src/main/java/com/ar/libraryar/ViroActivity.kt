package com.ar.libraryar


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.viro.core.*
import com.viro.core.Vector
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.*


/**
 * Activity that initializes Viro and ARCore. This activity builds an AR scene that continuously
 * detects planes. If you tap on a plane, an Android will appear at the location tapped. The
 * Androids are rendered using PBR (physically-based rendering).
 */
class ViroActivity : Activity() {
    private var mViroView: ViroViewARCore? = null
    var node: Node? = null
    var showhide: Button? = null
    private var controller: TrackedPlanesController? = null
    var positionMap = hashMapOf<Node, Vector>()
    var hide = false
    var boneMap = hashMapOf<String, BoundingBox>()
    var boundingBoxMap = hashMapOf<Vector, BoundingBox>()
    var exploded = false


    //    private var mViroView: ViroView? = null
    private var mScene: ARScene? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViroView = ViroViewARCore(this, object : ViroViewARCore.StartupListener {
            override fun onSuccess() {
                displayScene()
            }

            override fun onFailure(error: ViroViewARCore.StartupError?, errorMessage: String) {
                Log.e(
                    TAG,
                    "Error initializing AR [$errorMessage]"
                )
            }
        })
        setContentView(mViroView)
    }

    fun showPopup(view: View?) {
        Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()
    }

    /**
     * Create an AR scene that tracks planes. Tapping on a plane places a 3D Object on the spot
     * tapped.
     */
    private fun displayScene() {
        // Create the 3D AR scene, and display the point cloud
        mScene = ARScene()
        mScene?.displayPointCloud(true)

        val featurePoint = PointCloudUpdateListener { }
        // Create a TrackedPlanesController to visually display identified planes.
        controller = TrackedPlanesController(this, mViroView)
        // Spawn a 3D Droid on the position where the user has clicked on a tracked plane.
        Log.d("devhell", "displayScene: ${mScene?.rootNode}")
        mScene?.rootNode?.clickListener = object : ClickListener {

            override fun onClick(p0: Int, p1: Node?, p2: Vector?) {
                Log.d("devhell", "onClickState:$p1 ")
            }

            override fun onClickState(
                p0: Int,
                p1: Node?,
                p2: ClickState?,
                p3: Vector?
            ) {

            }

        }
        controller?.addOnPlaneClickListener(object : ClickListener {
            override fun onClick(p0: Int, p1: Node?, p2: Vector?) {
                if (p2 != null) {
//                    createDroidAtPosition(p2)
                    createDroid(p2, this)
//                    controller.removeOnPlaneClickListener(this)
                }
            }

            override fun onClickState(
                i: Int,
                node: Node?,
                clickState: ClickState?,
                vector: Vector?
            ) {
                //No-op
            }
        })
        mScene?.setListener(controller)
        // Add some lights to the scene; this will give the Android's some nice illumination.
        val rootNode: Node? = mScene?.rootNode
        val lightPositions: MutableList<Vector> = ArrayList<Vector>()
        lightPositions.add(Vector(-10.0, 10.0, 1.0))
        lightPositions.add(Vector(10.0, 10.0, 1.0))
        val intensity = 300f
        val lightColors: MutableList<Long> = ArrayList<Long>()
        lightColors.add((Color.WHITE).toLong())
        lightColors.add((Color.WHITE).toLong())
        for (i in lightPositions.indices) {
            val light = OmniLight()
            light.color = lightColors[i]
            light.position = lightPositions[i]
            light.attenuationStartDistance = 20F
            light.attenuationEndDistance = 30F
            light.intensity = intensity
            rootNode?.addLight(light)
        }

        //Add an HDR environment map to give the Android's more interesting ambient lighting.
        val environment: Texture =
            Texture.loadRadianceHDRTexture(Uri.parse("file:///android_asset/ibl_newport_loft.hdr"))
        mScene?.lightingEnvironment = environment
        mViroView?.scene = mScene
    }

    /**
     * Create an Android object and have it appear at the given location.
     * @param position The location where the Android should appear.
     */
    private fun createDroidAtPosition(position: Vector) {
        // Create a droid on the surface
//        val bot = getBitmapFromAsset(this, "andy.png")
        val object3D = Object3D()
//        object3D.setPosition(position)
//        mScene?.rootNode?.addChildNode(object3D)
        val listener = object3D.clickListener

        // Load the Android model asynchronously.
        object3D.loadModel(
            mViroView?.viroContext,
            Uri.parse("file:///android_asset/skeleton.glb"),
            Object3D.Type.GLB,
            object : AsyncObject3DListener {
                override fun onObject3DLoaded(model: Object3D?, type: Object3D.Type?) {
//                    Log.d("devhell", "onObject3DLoaded:${model?.physicsBody} ")
                    // When the model is loaded, set the texture associated with this OBJ
                    val childnode = model?.childNodes
                    val subnode = childnode?.count()
                    model?.clickListener = object : ClickListener {
                        override fun onClick(p0: Int, p1: Node?, p2: Vector?) {
                            Log.d("devhell", "onClick: $p1")
                        }

                        override fun onClickState(
                            p0: Int,
                            p1: Node?,
                            p2: ClickState?,
                            p3: Vector?
                        ) {
                            Log.d("devhell", "onClickState:$p3 ")
                        }

                    }
                    Log.d("devhell", "onObject3DLoaded:${subnode} ")
                    childnode?.forEach {
                        Log.d("devhell", "onObject3DLoaded: ${it.childNodes}")
                        it.childNodes.forEach { it2 ->
                            mScene?.rootNode?.addChildNode(it2)
                        }
                    }
//                    val objectTexture = Texture(bot, Texture.Format.RGBA8, false, false)
//                    val material = Material()
//                    material.setDiffuseTexture(objectTexture)

                    // Give the material a more "metallic" appearance, so it reflects the environment map.
                    // By setting its lighting model to PHYSICALLY_BASED, we enable PBR rendering on the
                    // model.
//                    material.roughness = 0.23f
//                    material.metalness = 0.7f
//                    material.lightingModel = Material.LightingModel.PHYSICALLY_BASED
//                    object3D.geometry.materials = Arrays.asList(material)
                }

                override fun onObject3DFailed(s: String?) {}
            })

//         Make the object draggable.
        node?.setDragListener { i, node, vector, vector1 ->
            // No-op.
        }
        node?.setDragType(Node.DragType.FIXED_DISTANCE)
    }


    private fun createDroid(position: Vector, param: ClickListener) {
        Log.d("devhell", "createDroid:$position ")
        // Create a droid on the surface
//        val bot = getBitmapFromAsset(this, "andy.png")
        node = Node()
        mScene?.rootNode?.addChildNode(node)
        node?.setPosition(position)

        val object3D = Object3D()
//        object3D.setPosition(p2)

        // Load the Android model asynchronously.
        object3D.loadModel(
            mViroView?.viroContext,
            Uri.parse("file:///android_asset/skeleton.glb"),
            Object3D.Type.GLB,
            object : AsyncObject3DListener {
                override fun onObject3DLoaded(model: Object3D?, type: Object3D.Type?) {
                    showhide = findViewById(R.id.show_hide)
                    Log.d("devhell", "onObject3DLoaded: Model Loaded")
                    controller?.removeOnPlaneClickListener(param)
                    // When the model is loaded, set the texture associated with this OBJ
                    val childnode = model?.childNodes
                    childnode?.forEach {
//                        Log.d("devhell", "onObject3DLoaded: ${it.childNodes}")
                        it.childNodes.forEach { it2 ->
                            it2.isVisible = true
                            node?.addChildNode(it2)
                            it2.setDragListener { i, node, vector, vector2 ->
                                showhide?.visibility = View.VISIBLE
                            }
//                            boneMap[it2.name] = it2.boundingBox
//                            boundingBoxMap[it2.positionRealtime] = it2.boundingBox
                            positionMap[it2] = it2.positionRealtime
                            Log.d("devhell", "de:$boneMap ")
                        }
                    }
                    mScene?.rootNode?.highAccuracyEvents = true

//                    showhide?.setOnClickListener { it ->
//                        it.isVisible = false
//                        node?.childNodes?.forEach { node ->
//
//                            var visibility = node.isVisible
//                            Log.d(
//                                "devhell", "rootnodesize:${
//                                    visibility
//                                } "
//                            )
//                            node.isVisible = visibility.not()
//                            Thread.sleep(200)
//                        }
//                        it.isVisible = true
//                        hide = true
//                    }
                    showhide?.setOnClickListener {
//                        if (!exploded) {
//                            node?.childNodes?.forEach {
//                                it.setPosition(
//                                    Vector(
//                                        it.positionRealtime.x+ Math.random(),
//                                        (it.positionRealtime.y.toDouble()),
//                                        it.positionRealtime.z.toDouble()
//                                    )
//                                )
//                            }
//                        } else {
                            node?.childNodes?.forEach {q1 ->
                                positionMap.forEach { q2 ->
                                    Log.d("devhell", "onObject3DLoaded: ")
                                        if(q2.key == q1){
                                            q1.setPosition(q2.value)
                                        }
                                }
                            }
//                        }
//                        exploded = exploded.not()
                    }
                    mScene?.rootNode?.setPosition(position)
                    mScene?.rootNode?.childNodes?.forEach {
                        Log.d("devhell", "name: ${it.name}")
                    }
//                    val objectTexture = Texture(bot, Texture.Format.RGBA8, false, false)
//                    val material = Material()
//                    material.setDiffuseTexture(objectTexture)

                    // Give the material a more "metallic" appearance, so it reflects the environment map.
                    // By setting its lighting model to PHYSICALLY_BASED, we enable PBR rendering on the
                    // model.
//                    material.roughness = 0.23f
//                    material.metalness = 0.7f
//                    material.lightingModel = Material.LightingModel.PHYSICALLY_BASED
//                    object3D.geometry.materials = Arrays.asList(material)
                    val portalscene = mScene?.rootNode as PortalScene
                    portalscene.clickListener = object : ClickListener {
                        override fun onClick(p0: Int, p1: Node?, p2: Vector?) {
                            Log.d("devhell", "onClick: ${p2}")
                            for (pos in boundingBoxMap) {
                                Log.d("amal", "onHitTestFinished${pos.key} ")
                                if (pos.key == p2?.normalize()) {
                                    Log.d("amal", "onHitTestFinished2${pos} ")
                                }
                            }

                        }

                        @SuppressLint("SuspiciousIndentation")
                        override fun onClickState(
                            p0: Int,
                            p1: Node?,
                            p2: ClickState?,
                            p3: Vector?
                        ) {
                            if (p2 == ClickState.CLICK_DOWN)
//                            CLICK_DOWN
//                            CLICK_UP
//                            CLICKED

//                                mViroView?.performARHitTestWithPosition(p3,object : ARHitTestListener{
//                                    override fun onHitTestFinished(p0: Array<out ARHitTestResult>?) {
//                                        Log.d("amal", "onHitTestFinished:$p0 ")
//                                        if(p0?.isNotEmpty() == true) {
//                                        var position = p0?.get(0)?.position
//                                        }
//                                        for(pos in boundingBoxMap){
//                                           if( pos.key == position){
//                                               Log.d("amal", "onHitTestFinished: ")
//                                           }
//                                        }
//
//                                    }
//
//                                })
                                Log.d("devhell", "onClickState:$")
                        }

                    }
                    val children = node?.childNodes
                    val randNode = children?.random()
//                    randNode?.isVisible = false
//                    Thread.sleep(800)
//                    randNode?.isVisible = true
//                    randNode?.setPosition(Vector(0.0,0.0,0.0))
                    // Make the object draggable.
//                    randNode?.setDragListener { i, node, vector, vector2 ->
//
//                    }
//                    randNode?.dragType = Node.DragType.FIXED_TO_WORLD
                }

                override fun onObject3DFailed(s: String?) {
                    Log.d("devhell", "onObject3DFailed:$s ")
                }
            })

    }

    /**
     * Tracks planes and renders a surface on them so the user can see where we've identified
     * planes.
     */
    class TrackedPlanesController(activity: Activity, rootView: View?) :
        ARScene.Listener {
        private val mCurrentActivityWeak: WeakReference<Activity>
        private var searchingForPlanesLayoutIsVisible = false
        private val surfaces: HashMap<String, Node> = HashMap<String, Node>()
        private val mPlaneClickListeners: MutableSet<ClickListener> = HashSet<ClickListener>()
        override fun onTrackingUpdated(
            trackingState: ARScene.TrackingState?,
            trackingStateReason: ARScene.TrackingStateReason?
        ) {
            //no-op
        }

        init {
            mCurrentActivityWeak = WeakReference(activity)
            // Inflate viro_view_hud.xml layout to display a "Searching for surfaces" text view.
            View.inflate(activity, R.layout.viro_view_hud, rootView as ViewGroup?)
        }

        fun addOnPlaneClickListener(listener: ClickListener) {
            mPlaneClickListeners.add(listener)
        }

        fun removeOnPlaneClickListener(listener: ClickListener) {
            if (mPlaneClickListeners.contains(listener)) {
                mPlaneClickListeners.remove(listener)
            }
        }

        /**
         * Once a Tracked plane is found, we can hide the our "Searching for Surfaces" UI.
         */
        private fun hideIsTrackingLayoutUI() {
            if (searchingForPlanesLayoutIsVisible) {
                return
            }
            searchingForPlanesLayoutIsVisible = true
            val activity = mCurrentActivityWeak.get() ?: return
            val isTrackingFrameLayout = activity.findViewById<View>(R.id.viro_view_hud)
            isTrackingFrameLayout.animate().alpha(0.0f).duration = 2000
        }

        override fun onAnchorFound(arAnchor: ARAnchor, arNode: ARNode) {
            // Spawn a visual plane if a PlaneAnchor was found
            if (arAnchor.type === ARAnchor.Type.PLANE) {
                val planeAnchor: ARPlaneAnchor = arAnchor as ARPlaneAnchor

                // Create the visual geometry representing this plane
                val dimensions: Vector = planeAnchor.extent
                val plane = Quad(1F, 1F)
                plane.width = dimensions.x
                plane.height = dimensions.z

                // Set a default material for this plane.
                val material = Material()
                material.diffuseColor = Color.parseColor("#BF000000")
                plane.materials = listOf(material)

                // Attach it to the node
                val planeNode = Node()
                planeNode.geometry = plane
                planeNode.setRotation(Vector(-Math.toRadians(90.0), 0.0, 0.0))
                planeNode.setPosition(planeAnchor.center)

                // Attach this planeNode to the anchor's arNode
                arNode.addChildNode(planeNode)
                surfaces[arAnchor.getAnchorId()] = planeNode

                // Attach click listeners to be notified upon a plane onClick.
                planeNode.clickListener = object : ClickListener {
                    override fun onClick(i: Int, node: Node?, vector: Vector?) {
                        for (listener in mPlaneClickListeners) {
                            listener.onClick(i, node, vector)
                        }
                    }

                    override fun onClickState(
                        i: Int,
                        node: Node?,
                        clickState: ClickState?,
                        vector: Vector?
                    ) {
                        //No-op
                    }
                }

                // Finally, hide isTracking UI if we haven't done so already.
                hideIsTrackingLayoutUI()
            }
        }

        override fun onAnchorUpdated(arAnchor: ARAnchor, arNode: ARNode?) {
            if (arAnchor.type === ARAnchor.Type.PLANE) {
                val planeAnchor: ARPlaneAnchor = arAnchor as ARPlaneAnchor

                // Update the mesh surface geometry
                val node: Node? = surfaces[arAnchor.getAnchorId()]
                val plane: Quad = node?.geometry as Quad
                val dimensions: Vector = planeAnchor.extent
                plane.width = dimensions.x
                plane.height = dimensions.z
            }
        }

        override fun onAnchorRemoved(arAnchor: ARAnchor, arNode: ARNode?) {
            surfaces.remove(arAnchor.anchorId)
        }

        override fun onTrackingInitialized() {
            //No-op
        }

        override fun onAmbientLightUpdate(lightIntensity: Float, lightColor: Vector?) {
            //No-op
        }
    }

    private fun getBitmapFromAsset(context: Context, assetName: String): Bitmap? {
        val assetManager = context.resources.assets
        val imageStream: InputStream = try {
            assetManager.open(assetName)
        } catch (exception: IOException) {
            Log.w(
                TAG, "Unable to find image [" + assetName + "] in assets! Error: "
                        + exception.message
            )
            return null
        }
        return BitmapFactory.decodeStream(imageStream)
    }

    override fun onStart() {
        super.onStart()
        mViroView?.onActivityStarted(this)
    }

    override fun onResume() {
        super.onResume()
        mViroView?.onActivityResumed(this)
    }

    override fun onPause() {
        super.onPause()
        mViroView?.onActivityPaused(this)
    }

    override fun onStop() {
        super.onStop()
        mViroView?.onActivityStopped(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mViroView?.onActivityDestroyed(this)
    }

    companion object {
        private val TAG = ViroActivity::class.java.simpleName

    }
}

