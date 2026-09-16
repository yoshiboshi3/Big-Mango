using UnityEngine;

public sealed class HostedAppFrameSource : MonoBehaviour
{
    public Renderer TargetRenderer;
    public AndroidBridge Bridge;
    public Texture2D Texture { get; private set; }
    long lastFrame = -1;

    public void Initialize(AndroidBridge bridge)
    {
        Bridge = bridge;
        Texture = new Texture2D(960, 540, TextureFormat.RGBA32, false, false);
        Texture.wrapMode = TextureWrapMode.Clamp;
        if (TargetRenderer != null)
        {
            var material = TargetRenderer.material;
            material.mainTexture = Texture;
            material.mainTextureScale = new Vector2(1f, -1f);
            material.mainTextureOffset = new Vector2(0f, 1f);
        }
    }

    void Update()
    {
        if (Bridge == null || Texture == null) return;
        long count = Bridge.FrameCount();
        if (count <= 0 || count == lastFrame) return;
        var bytes = Bridge.CopyLatestFrame();
        if (bytes == null || bytes.Length != 960 * 540 * 4) return;
        Texture.LoadRawTextureData(bytes);
        Texture.Apply(false, false);
        lastFrame = count;
    }
}
