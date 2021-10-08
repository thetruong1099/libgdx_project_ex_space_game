package com.dds.spirogame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Locale;

public class GameScreen implements Screen {
    
    //screen
    private Camera camera;
    private ShapeRenderer sr;
    private Viewport viewport;

    //graphics
    private SpriteBatch batch;
    private TextureAtlas textureAtlas;

    private Texture explosionTexture;

    private TextureRegion[] backgrounds;

    private TextureRegion playerShipTextureRegion, playerShieldTextureRegion, playerLaserTextureRegion,
            enemyShipTextureRegion, enemyShieldTextureRegion, enemyLaserTextureRegion;

    //timing
    private float[] backgroundOffsets = {0, 0, 0, 0};
    private float backgroundMaxScrollingSpeed;
    private float backgroundHeight;
    private float timeBetweenEnemySpawns = 1f;
    private float enemySpawnTimer = 0;

    //world parameters
    private final float WORLD_WIDTH = 72;
    private final float WORLD_HEIGHT = 128;
    private final float TOUCH_MOVEMENT_THRESHOLD = 0.5f;

    //game object
    private PlayerShip playerShip;
    private LinkedList<EnemyShip> enemyShipList;
    private LinkedList<Laser> playerLaserList;
    private LinkedList<Laser> enemyLaserList;
    private LinkedList<Explosion> explosions;
    private int score = 0;

    //Heads-Up Display
    BitmapFont font;
    float hubVerticalMargin, hubLeftX, hubRightX, hubCentreX, hubRow1Y, hubRow2Y, hubSectionWidth;


    GameScreen() {
        camera = new OrthographicCamera();
        viewport = new StretchViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);

        //setup the texture atlas
        textureAtlas = new TextureAtlas("Images.atlas");

        //setting up background
        backgrounds = new TextureRegion[4];
        backgrounds[0] = textureAtlas.findRegion("starscape00");
        backgrounds[1] = textureAtlas.findRegion("starscape01");
        backgrounds[2] = textureAtlas.findRegion("starscape02");
        backgrounds[3] = textureAtlas.findRegion("starscape03");

        backgroundHeight = WORLD_HEIGHT * 2;
        backgroundMaxScrollingSpeed = (float) WORLD_HEIGHT / 4;

        //initialize texture region
        playerShipTextureRegion = textureAtlas.findRegion("playerShip2_blue");
        playerShieldTextureRegion = textureAtlas.findRegion("shield1");
        playerLaserTextureRegion = textureAtlas.findRegion("laserBlue03");
        enemyShipTextureRegion = textureAtlas.findRegion("enemyRed3");
        enemyShieldTextureRegion = textureAtlas.findRegion("shield2");
        enemyShieldTextureRegion.flip(false, true);
        enemyLaserTextureRegion = textureAtlas.findRegion("laserRed03");

        explosionTexture = new Texture("explosion.png");
        //setup game object
        playerShip = new PlayerShip(50, 3,
                10, 10,
                WORLD_WIDTH / 2, WORLD_HEIGHT / 4,
                0.4f, 4, 45, 0.5f,
                playerShipTextureRegion, playerShieldTextureRegion, playerLaserTextureRegion);

        enemyShipList = new LinkedList<>();


        playerLaserList = new LinkedList<>();
        enemyLaserList = new LinkedList<>();
        explosions = new LinkedList<>();

        batch = new SpriteBatch();

        prepareHUD();
    }

    private void prepareHUD() {
        //Create a BitmapFont from file
        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("EdgeOfTheGalaxyRegular-OVEa6.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter fontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        fontParameter.size = 72;
        fontParameter.borderWidth = 3.6f;
        fontParameter.color = new Color(1, 1, 1, 0.3f);
        fontParameter.borderColor = new Color(0, 0, 0, 0.3f);

        font = fontGenerator.generateFont(fontParameter);

        //scale the font to fit world

        font.getData().setScale(0.08f);

        //calculate hub margins, etc.
        hubVerticalMargin = font.getCapHeight() / 2;
        hubLeftX = hubVerticalMargin;
        hubRightX = WORLD_WIDTH * 2 / 3 - hubLeftX;
        hubCentreX = WORLD_WIDTH / 3;
        hubRow1Y = WORLD_HEIGHT - hubVerticalMargin;
        hubRow2Y = hubRow1Y - hubVerticalMargin - font.getCapHeight();
        hubSectionWidth = WORLD_WIDTH / 3;

    }


    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        batch.begin();

        //scrolling background;
        renderBackground(delta);

        detectInput(delta);
        playerShip.update(delta);

        spawnEnemyShips(delta);

        ListIterator<EnemyShip> enemyShipListIterator = enemyShipList.listIterator();
        while (enemyShipListIterator.hasNext()) {
            EnemyShip enemyShip = enemyShipListIterator.next();
            moveEnemies(enemyShip, delta);
            enemyShip.update(delta);
            //enemy ship
            enemyShip.draw(batch);
        }


        //player ship
        playerShip.draw(batch);
        //lasers
        renderLasers(delta);
        //detect collisions between lasers and ships
        detectCollisions();
        //explosions
        updateAndRenderExplosions(delta);
        //hub rendering
        updateAndRenderHud(delta);

        batch.end();
    }

    private void updateAndRenderHud(float delta) {
        //render top row labels
        font.draw(batch, "Score", hubLeftX, hubRow1Y, hubSectionWidth, Align.left, false);
        font.draw(batch, "Shield", hubCentreX, hubRow1Y, hubSectionWidth, Align.center, false);
        font.draw(batch, "Live", hubRightX, hubRow1Y, hubSectionWidth, Align.right, false);
        //render second row labels
        font.draw(batch, String.format(Locale.getDefault(), "%06d", score),
                hubLeftX, hubRow2Y, hubSectionWidth, Align.left, false);
        font.draw(batch, String.format(Locale.getDefault(), "%02d", playerShip.shield),
                hubCentreX, hubRow2Y, hubSectionWidth, Align.center, false);
        font.draw(batch, String.format(Locale.getDefault(), "%02d", playerShip.lives),
                hubRightX, hubRow2Y, hubSectionWidth, Align.right, false);
    }

    private void spawnEnemyShips(float deltaTime) {

        enemySpawnTimer += deltaTime;

        if (enemySpawnTimer > timeBetweenEnemySpawns) {
            enemyShipList.add(new EnemyShip(50, 1, 10, 10,
                    SpiroGame.random.nextFloat() * (WORLD_WIDTH - 10) + 5,
                    WORLD_HEIGHT - 5,
                    0.3f, 5, 50, 0.8f,
                    enemyShipTextureRegion, enemyShieldTextureRegion, enemyLaserTextureRegion));
            enemySpawnTimer -= timeBetweenEnemySpawns;
        }
    }

    private void moveEnemies(EnemyShip enemyShip, float delta) {
        //strategy: determine the max distance the ship can move
        float leftLimit, rightLimit, upLimit, downLimit;
        leftLimit = -enemyShip.boundingBox.x;
        downLimit = (float) WORLD_HEIGHT / 2 - enemyShip.boundingBox.y;
        rightLimit = WORLD_WIDTH - enemyShip.boundingBox.x - enemyShip.boundingBox.width;
        upLimit = WORLD_HEIGHT - enemyShip.boundingBox.y - enemyShip.boundingBox.height;

        //scale to the maximum speed of the ship
        float xMove = enemyShip.getDirectionVector().x * enemyShip.movementSpeed * delta;
        float yMove = enemyShip.getDirectionVector().y * enemyShip.movementSpeed * delta;

        if (xMove > 0) xMove = Math.min(xMove, rightLimit);
        else xMove = Math.max(xMove, leftLimit);

        if (yMove > 0) yMove = Math.min(yMove, upLimit);
        else yMove = Math.max(yMove, downLimit);

        enemyShip.translate(xMove, yMove);
    }

    private void detectInput(float delta) {
        //keyboard input
        //strategy: determine the max distance the ship can move
        //check each key that matters and move accordingly
        float leftLimit, rightLimit, upLimit, downLimit;
        leftLimit = -playerShip.boundingBox.x;
        downLimit = -playerShip.boundingBox.y;
        rightLimit = WORLD_WIDTH - playerShip.boundingBox.x - playerShip.boundingBox.width;
        upLimit = (float) WORLD_HEIGHT / 2 - playerShip.boundingBox.y - playerShip.boundingBox.height;

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) && rightLimit > 0) {
            playerShip.translate(Math.min(playerShip.movementSpeed * delta, rightLimit), 0f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.UP) && upLimit > 0) {
            playerShip.translate(0f, Math.min(playerShip.movementSpeed * delta, upLimit));
        }

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) && leftLimit < 0) {
            playerShip.translate(Math.max(-playerShip.movementSpeed * delta, leftLimit), 0f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) && downLimit < 0) {
            playerShip.translate(0f, Math.max(-playerShip.movementSpeed * delta, downLimit));
        }

        //touch input (also mouse)
        if (Gdx.input.isTouched()) {
            //get screen position of touch
            float xTouchPixels = Gdx.input.getX();
            float yTouchPixels = Gdx.input.getY();

            //convert to world position
            Vector2 touchPoint = new Vector2(xTouchPixels, yTouchPixels);
            touchPoint = viewport.unproject(touchPoint);

            //calculate the x and y differences
            Vector2 playerShipCentre = new Vector2(
                    playerShip.boundingBox.x + playerShip.boundingBox.width / 2,
                    playerShip.boundingBox.y + playerShip.boundingBox.height / 2);

            float touchDistance = touchPoint.dst(playerShipCentre);

            if (touchDistance > TOUCH_MOVEMENT_THRESHOLD) {
                float xTouchDifference = touchPoint.x - playerShipCentre.x;
                float yTouchDifference = touchPoint.y - playerShipCentre.y;

                //scale to the maximum speed of the ship
                float xMove = xTouchDifference / touchDistance * playerShip.movementSpeed * delta;
                float yMove = yTouchDifference / touchDistance * playerShip.movementSpeed * delta;

                if (xMove > 0) xMove = Math.min(xMove, rightLimit);
                else xMove = Math.max(xMove, leftLimit);

                if (yMove > 0) yMove = Math.min(yMove, upLimit);
                else yMove = Math.max(yMove, downLimit);

                playerShip.translate(xMove, yMove);

            }

        }
    }

    private void detectCollisions() {
        //for each player laser, check whether, it intersects an enemy ship
        ListIterator<Laser> laserListIterator = playerLaserList.listIterator();
        while (laserListIterator.hasNext()) {
            Laser laser = laserListIterator.next();
            ListIterator<EnemyShip> enemyShipListIterator = enemyShipList.listIterator();
            while (enemyShipListIterator.hasNext()) {
                EnemyShip enemyShip = enemyShipListIterator.next();
                if (enemyShip.intersects(laser.boundingBox)) {
                    //contact with enemy ship
                    if (enemyShip.hitAndCheckDestroyed(laser)) {
                        enemyShipListIterator.remove();
                        explosions.add(new Explosion(explosionTexture,
                                new Rectangle(enemyShip.boundingBox),
                                0.7f));
                        score += 100;
                    }
                    laserListIterator.remove();
                    break;
                }
            }
        }
        laserListIterator = enemyLaserList.listIterator();
        while (laserListIterator.hasNext()) {
            Laser laser = laserListIterator.next();
            if (playerShip.intersects(laser.boundingBox)) {
                //contact with player ship
                if (playerShip.hitAndCheckDestroyed(laser)) {
                    explosions.add(new Explosion(explosionTexture,
                            new Rectangle(playerShip.boundingBox),
                            1.6f));
                    playerShip.shield = 10;
                    playerShip.lives--;
                }
                ;
                laserListIterator.remove();
            }
        }
        //for each enemy laser, check whether, it intersects an player ship
    }

    private void updateAndRenderExplosions(float delta) {
        ListIterator<Explosion> explosionListIterator = explosions.listIterator();
        while (explosionListIterator.hasNext()) {
            Explosion explosion = explosionListIterator.next();
            explosion.update(delta);
            if (explosion.isFinished()) {
                explosionListIterator.remove();
            } else {
                explosion.draw(batch);
            }
        }
    }

    private void renderLasers(float delta) {
        //create news lasers
        //player lasers
        if (playerShip.canFireLaser()) {
            Laser[] lasers = playerShip.fireLasers();
            for (Laser laser : lasers) {
                playerLaserList.add(laser);
            }
        }
        //enemy laser
        ListIterator<EnemyShip> enemyShipListIterator = enemyShipList.listIterator();
        while (enemyShipListIterator.hasNext()) {
            EnemyShip enemyShip = enemyShipListIterator.next();
            if (enemyShip.canFireLaser()) {
                Laser[] lasers = enemyShip.fireLasers();
                for (Laser laser : lasers) {
                    enemyLaserList.add(laser);
                }
            }
        }
        //draw laser
        //remove old lasers
        ListIterator<Laser> iterator = playerLaserList.listIterator();
        while (iterator.hasNext()) {
            Laser laser = iterator.next();
            laser.draw(batch);
            laser.boundingBox.y += laser.movementSpeed * delta;
            if (laser.boundingBox.y > WORLD_HEIGHT) {
                iterator.remove();
            }
        }

        iterator = enemyLaserList.listIterator();
        while (iterator.hasNext()) {
            Laser laser = iterator.next();
            laser.draw(batch);
            laser.boundingBox.y -= laser.movementSpeed * delta;
            if (laser.boundingBox.y + laser.boundingBox.height < 0) {
                iterator.remove();
            }
        }
    }

    private void renderBackground(float delta) {
        backgroundOffsets[0] += delta * backgroundMaxScrollingSpeed / 8;
        backgroundOffsets[1] += delta * backgroundMaxScrollingSpeed / 4;
        backgroundOffsets[2] += delta * backgroundMaxScrollingSpeed / 2;
        backgroundOffsets[3] += delta * backgroundMaxScrollingSpeed;

        for (int layer = 0; layer < backgroundOffsets.length; layer++) {
            if (backgroundOffsets[layer] > WORLD_HEIGHT) {
                backgroundOffsets[layer] = 0;
            }
            batch.draw(backgrounds[layer], 0, -backgroundOffsets[layer], WORLD_WIDTH, backgroundHeight);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
