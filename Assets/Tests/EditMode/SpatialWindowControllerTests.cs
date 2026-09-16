using NUnit.Framework;
using UnityEngine;

public class SpatialWindowControllerTests
{
    [Test]
    public void Apply_MapsStateToWorldTransform()
    {
        var go = new GameObject("window");
        var c = go.AddComponent<SpatialWindowController>();
        c.Apply(new SpatialWindowState(0.4f, -0.2f, 2.5f, 1.25f));
        Assert.That(go.transform.localPosition, Is.EqualTo(new Vector3(0.4f, -0.2f, 2.5f)));
        Assert.That(go.transform.localScale, Is.EqualTo(Vector3.one * 1.25f));
        Object.DestroyImmediate(go);
    }

    [Test]
    public void Center_ResetsXYScaleAndUsesRequestedDepth()
    {
        var go = new GameObject("window");
        var c = go.AddComponent<SpatialWindowController>();
        c.Apply(new SpatialWindowState(1f, 1f, 5f, 3f));
        c.Center(2f);
        Assert.That(go.transform.localPosition, Is.EqualTo(new Vector3(0f, 0f, 2f)));
        Assert.That(go.transform.localScale, Is.EqualTo(Vector3.one));
        Object.DestroyImmediate(go);
    }
}
