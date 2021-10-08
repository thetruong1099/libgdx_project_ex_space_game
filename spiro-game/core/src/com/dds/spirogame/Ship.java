package com.dds.spirogame;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

abstract class Ship {

    //ship characteristics
    float movementSpeed; //world units per second
    int shield;

    //position & dimension
    Rectangle boundingBox;

    //laser animation
    float laserWidth, laserHeight;
    float laserMovementSpeed;
    float timeBetweenShoot;
    float timeSinceLastShot = 0;

    //graphic
    TextureRegion shipTextureRegion, shieldTextureRegion, laserTextureRegion;

    public Ship(float movementSpeed,
                int shield,
                float width, float height,
                float xCentre, float yCentre,
                float laserWidth, float laserHeight, float laserMovementSpeed,
                float timeBetweenShoot,
                TextureRegion shipTextureRegion,
                TextureRegion shieldTextureRegion,
                TextureRegion laserTextureRegion) {
        this.movementSpeed = movementSpeed;
        this.shield = shield;
        this.boundingBox = new Rectangle(xCentre - width / 2, yCentre - height / 2, width, height);
        this.laserWidth = laserWidth;
        this.laserHeight = laserHeight;
        this.laserMovementSpeed = laserMovementSpeed;
        this.timeBetweenShoot = timeBetweenShoot;
        this.shipTextureRegion = shipTextureRegion;
        this.shieldTextureRegion = shieldTextureRegion;
        this.laserTextureRegion = laserTextureRegion;
    }

    public void update(float deltaTime) {
        timeSinceLastShot += deltaTime;
    }

    public abstract Laser[] fireLasers();

    public boolean canFireLaser() {
        return (timeSinceLastShot - timeBetweenShoot >= 0);
    }

    public boolean intersects(Rectangle rectangle) {
        return boundingBox.overlaps(rectangle);
    }

    public void draw(Batch batch) {
    }

    public boolean hitAndCheckDestroyed(Laser laser) {
        if (shield > 0) {
            shield--;
            return false;
        }
        return true;
    }

    public void translate(float xChange, float yChange) {
        boundingBox.setPosition(boundingBox.x + xChange, boundingBox.y + yChange);
    }
}
