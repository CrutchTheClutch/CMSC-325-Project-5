package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import static com.jme3.bullet.PhysicsSpace.getPhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.scene.CameraNode;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;

/**
 * Main
 */
public class Main extends SimpleApplication implements PhysicsCollisionListener {

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    
    /* Physics Application State (jBullet) */
    private BulletAppState bulletAppState;
    
    /* Nodes */
    protected Node playerNode;
    protected Spatial player;
    protected Node terrainNode;
    protected Spatial terrain;
    
    /* Materials */
    Material target_mat;
    Material targetHit_mat;
    Material terrain_mat;
    Material proj_mat;
    
    /* Physics Nodes */
    private RigidBodyControl    target_phy;
    private RigidBodyControl    proj_phy;
    
    /* Geometries */
    private static final Box    box;
    private static final Sphere sphere;
    
    /* Speeds */
    private final float tSpeed = 5;     // Turning Speed
    private final float mSpeed = 15;    // Movement Speed
    private final float pSpeed = 30;    // Projectile Speed
    
    /* Target Dimensions */
    private static final float TARGET_LENGTH = 0.25f;
    private static final float TARGET_HEIGHT = 0.25f;
    private static final float TARGET_WIDTH = 0.25f;
    
    /* Target Count */
    int targetCount = 0;
    
    static {
        box = new Box(TARGET_LENGTH, TARGET_HEIGHT, TARGET_WIDTH);
        sphere = new Sphere(32, 32, 0.1f, true, false);
    }

    @Override
    public void simpleInitApp() {
        // Set up Physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.setDebugEnabled(true);
        
        // Remove Default GUI
        setDisplayStatView(false); 
        setDisplayFps(false);
        
        initMaterials();
        
        initTerrain();
        
        initPlayer();
        
        // Create 3 Targets
        createTarget(10, 0.1f, 5);
        createTarget(15, 0.1f, 10);
        createTarget(10, 0.1f, 15);
        createTarget(-10, 0.1f, 10);
        
        // Create Lighting
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1f, -1f, -1.0f));
        rootNode.addLight(sun);
        
        // Add Skybox
        rootNode.attachChild(
                SkyFactory.createSky(getAssetManager(), 
                "Textures/Sky/Bright/BrightSky.dds", 
                SkyFactory.EnvMapType.CubeMap));
        
        // Add Controls
        initControls();
        
        updateTargetGUI();
        
        getPhysicsSpace().addCollisionListener(this);
    }
    
    /** Initializes all Materials */
    public void initMaterials() {
        /** Textures */
        // Load Textures
        Texture dirt = assetManager.loadTexture(
            "Textures/Terrain/splat/dirt.jpg");
        Texture grass = assetManager.loadTexture(
            "Textures/Terrain/splat/grass.jpg");
        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/Rock/Rock.PNG");
        
        // Set Texture Wrap
        grass.setWrap(WrapMode.Repeat);
        dirt.setWrap(WrapMode.Repeat);
        
        /** Target */
        // Target Material
        target_mat = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");
        target_mat.setColor("Color", ColorRGBA.Yellow);
        
        // Target Hit Material
        targetHit_mat = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");
        targetHit_mat.setColor("Color", ColorRGBA.Red);
        
        /** Terrain */
        // Create Terrain Material
        terrain_mat = new Material(assetManager,
            "Common/MatDefs/Terrain/Terrain.j3md");

        // Add Terrain Alpha Map
        terrain_mat.setTexture("Alpha", assetManager.loadTexture(
            "Textures/Terrain/splat/alpha1.png"));
        
        // Apply Terrain Textures
        terrain_mat.setTexture("Tex1", grass);
        terrain_mat.setFloat("Tex1Scale", 64f);
        terrain_mat.setTexture("Tex2", grass);
        terrain_mat.setFloat("Tex2Scale", 64f);
        terrain_mat.setTexture("Tex3", dirt);
        terrain_mat.setFloat("Tex3Scale", 128f);
        
        /** Projectile */
        proj_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
        key2.setGenerateMips(true);
        Texture tex2 = assetManager.loadTexture(key2);
        proj_mat.setTexture("ColorMap", tex2);
        
    }
    
    /** Initializes Terrain */
    private void initTerrain() {
        // Create Terrain node
        terrainNode = new Node("world");
        
        // Create Spatial
        terrain = assetManager.loadModel(
                "Models/Terrain/Terrain.mesh.xml");
        
        // Set Material
        terrain.setMaterial(terrain_mat);
        
        terrain.scale(50);
        terrain.setLocalTranslation(0,-1,0);
        terrainNode.attachChild(terrain);
        
        // Attatch to rootNode
        rootNode.attachChild(terrainNode);
        
        // Add Physics
        terrain.addControl(new RigidBodyControl (0));
        bulletAppState.getPhysicsSpace().addAll(terrain);
    }
    
    /** Initializes Player */
    private void initPlayer() {
        // Create playerNode
        playerNode = new Node();
        
        // Create Player
        player = assetManager.loadModel(
                "Models/HoverTank/Tank2.mesh.xml");
        player.scale(0.1f);
        
        // New Quaternion
        Quaternion q = new Quaternion();
        q.fromAngles(0, 1.5708f, 0);
        
        // Player Position
        player.setLocalTranslation(0,-0.5f,0);
        player.setLocalRotation(q);
        
        // Attatch to playerNode
        playerNode.attachChild(player);
        
        // Create Player Camera
        flyCam.setEnabled(false);
        CameraNode camNode = new CameraNode("Camera Node", cam);
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        playerNode.attachChild(camNode);
        
        // Set Camera Position
        camNode.setLocalTranslation(new Vector3f(-4,.35f,0));
        camNode.setLocalRotation(q);
        
        // Set Player Location
        playerNode.setLocalTranslation(new Vector3f(0,0,10));
        
        // Attatch to rootNode
        rootNode.attachChild(playerNode);
    }
    
    /** Creates a Target
     * @param x coord for vec3
     * @param y coord for vec3
     * @param z coord for vec3 
     */
    public void createTarget(float x, float y, float z) {
        Geometry target_geo = new Geometry("target", box);
        target_geo.setMaterial(target_mat);
        rootNode.attachChild(target_geo);
        targetCount += 1; // Adds to target count (GUI)
        target_geo.setLocalTranslation(x, y, z);
        target_phy = new RigidBodyControl(1.0f);
        target_geo.addControl(target_phy);
        bulletAppState.getPhysicsSpace().add(target_phy);
    }
    
    public void shootProj() {
        Geometry proj_geo = new Geometry("proj", sphere);
        proj_geo.setMaterial(proj_mat);
        rootNode.attachChild(proj_geo);
        proj_geo.setLocalTranslation(player.getWorldTranslation().add(0,.3f,0));
        proj_phy = new RigidBodyControl(1.0f);
        proj_geo.addControl(proj_phy);
        bulletAppState.getPhysicsSpace().add(proj_phy);
        proj_phy.setLinearVelocity(cam.getDirection().mult(pSpeed));
    }
    
    /** GUI element that displays number of remaining targets */
    protected void updateTargetGUI() {
        // Removes GUI
        guiNode.detachAllChildren();
    
        // Set Font, Size, Position
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setLocalTranslation( 10, ch.getLineHeight() + 15, 0);
    
        // Sets Text (Num of Remaining Targets)
        ch.setText("Remaining Targets: " + targetCount);
        
        // You Win!
        if (targetCount == 0) {
            BitmapText winner = new BitmapText(guiFont, false);
            winner.setSize(guiFont.getCharSet().getRenderedSize() * 3);
            winner.setLocalTranslation( // center
            settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
            settings.getHeight() / 2 + winner.getLineHeight() / 2, 0);
            winner.setText("You Win!");
            
            guiNode.attachChild(winner);
        }
    
        // Attatch to GUI
        guiNode.attachChild(ch);
  }
    
    private void initControls() {
        inputManager.addMapping("Forward",  new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("RotateL",  new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Back",  new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("RotateR",  new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        
        // Add Listeners
        inputManager.addListener(analogListener,"Forward", "RotateL", "Back", "RotateR");
        inputManager.addListener(actionListener, "Shoot");
    }
    
    private final AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            Vector3f v = playerNode.getLocalRotation().getRotationColumn(0);
            
            if (name.equals("Forward")) {
                playerNode.move(v.mult(tpf).mult(mSpeed));
            }
            if (name.equals("RotateL")) {
                playerNode.rotate(0, value*tSpeed, 0);
            }
            if (name.equals("Back")) {
                playerNode.move(v.mult(tpf).mult(-mSpeed));
            }
            if (name.equals("RotateR")) {
                playerNode.rotate(0, -value*tSpeed, 0);
            }
        }
    };
    
    private final ActionListener actionListener = new ActionListener() {
    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {
      if (name.equals("Shoot") && !keyPressed) {
        shootProj();
      }
    }
  };
    

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    @Override
    public void collision(PhysicsCollisionEvent event) {
        if ("target".equals(event.getNodeA().getName())) {
            if ("proj".equals(event.getNodeB().getName())) {
                event.getNodeA().setMaterial(targetHit_mat);
                event.getNodeA().setName("targetHit");
                targetCount -= 1;
                updateTargetGUI();
                fpsText.setText(event.getNodeA().getName());
            }
        }
        if ("target".equals(event.getNodeB().getName())) {
            if ("proj".equals(event.getNodeA().getName())) {
                event.getNodeB().setMaterial(targetHit_mat);
                event.getNodeB().setName("targetHit");
                targetCount -= 1;
                updateTargetGUI();
                fpsText.setText(event.getNodeA().getName());
            }
        }
    }
}
