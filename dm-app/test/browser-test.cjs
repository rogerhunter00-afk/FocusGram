// Run with NODE_PATH pointing to a Playwright installation. No Instagram login.
const {chromium} = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({headless:true, ...(process.env.CHROME_PATH ? {executablePath:process.env.CHROME_PATH} : {})});
  const page = await browser.newPage({viewport:{width:390,height:844}});
  const html = `<html><head></head><body><nav>
    <a id="home" href="/">Home</a><a id="reels" href="/reels/">Reels</a>
    <a id="explore" href="/explore/">Explore</a><a id="stories" href="/stories/x/">Stories</a>
    <a id="profile" href="/alice/">Profile</a><a id="dm" href="/direct/t/123/">Chat</a></nav>
    <input id="search" placeholder="Search"><input id="photo" type="file" accept="image/*">
    <img id="image" alt="Photo in chat"><audio id="voice" controls></audio>
    <textarea id="message" placeholder="Message"></textarea><a id="external" href="https://example.com/">External</a>
    </body></html>`;
  let inboxLoads = 0;
  await page.route('https://www.instagram.com/**', async route => {
    if (new URL(route.request().url()).pathname === '/direct/inbox/') inboxLoads++;
    await route.fulfill({contentType:'text/html',body:html});
  });
  const script = fs.readFileSync(path.join(__dirname,'../assets/dm-only.js'),'utf8');
  await page.addInitScript(script);
  await page.goto('https://www.instagram.com/direct/inbox/');
  await page.waitForTimeout(250);
  for (const id of ['home','reels','explore','stories','profile']) assert.equal(await page.locator('#'+id).isVisible(),false,id);
  for (const id of ['dm','search','photo','image','voice','message','external']) assert.equal(await page.locator('#'+id).isVisible(),true,id);
  await page.evaluate(() => history.pushState({},'', '/direct/t/123/'));
  assert.ok(page.url().endsWith('/direct/t/123/'));
  await page.evaluate(() => history.pushState({},'', '/reels/'));
  await page.waitForURL('**/direct/inbox/');
  assert.ok(inboxLoads >= 2,'SPA route escaped instead of returning to inbox');
  await page.evaluate(() => {
    const a=document.createElement('a'); a.id='dynamic'; a.href='/explore/'; a.textContent='Dynamic Explore'; document.body.appendChild(a);
  });
  await page.waitForTimeout(250);
  assert.equal(await page.locator('#dynamic').isVisible(),false);
  await page.goto('https://www.instagram.com/accounts/login/');
  await page.waitForTimeout(150);
  assert.ok(page.url().endsWith('/accounts/login/'),'login route should stay open');
  await page.goto('https://www.instagram.com/stories/abc/');
  await page.waitForURL('**/direct/inbox/');
  console.log('Passed browser checks: navigation hiding, preserved chat controls, SPA redirect, dynamic links, auth and Stories redirects.');
  await browser.close();
})().catch(error=>{console.error(error);process.exit(1);});
