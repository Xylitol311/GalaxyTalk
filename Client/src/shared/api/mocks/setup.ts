export default async function enableMocking() {
    const { worker } = await import('./browser.ts');
    return worker.start({
        onUnhandledRequest: 'bypass',
    });
}
