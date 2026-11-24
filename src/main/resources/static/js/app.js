// StyleHub Client-side JavaScript enhancements

document.addEventListener('DOMContentLoaded', () => {

  // 1. Wishlist Toggle Handlers
  document.querySelectorAll('.btn-wishlist-toggle').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.preventDefault();
      const productId = btn.dataset.productId;
      if (!productId) return;

      try {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;

        const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (token && header) headers[header] = token;

        const res = await fetch('/wishlist/toggle', {
          method: 'POST',
          headers,
          body: new URLSearchParams({ productId })
        });

        if (res.status === 401) {
          window.location.href = '/login';
          return;
        }

        const data = await res.json();
        if (data.status === 'success') {
          if (data.added) {
            btn.classList.add('active');
            btn.innerHTML = '<i class="bi bi-heart-fill text-danger"></i>';
          } else {
            btn.classList.remove('active');
            btn.innerHTML = '<i class="bi bi-heart"></i>';
          }

          // Update header count
          const countBadge = document.querySelector('#wishlist-count-badge');
          if (countBadge) countBadge.textContent = data.wishlistCount;
        }
      } catch (err) {
        console.error('Wishlist error:', err);
      }
    });
  });

  // 2. Product Gallery Thumbnail Switcher
  const mainImage = document.querySelector('#main-product-image');
  document.querySelectorAll('.thumb-selector').forEach(thumb => {
    thumb.addEventListener('click', () => {
      document.querySelectorAll('.thumb-selector').forEach(t => t.classList.remove('active'));
      thumb.classList.add('active');
      if (mainImage) mainImage.src = thumb.dataset.largeUrl;
    });
  });

  // 3. Razorpay Checkout Handler (Checkout Page)
  const razorpayTriggerBtn = document.querySelector('#btn-razorpay-trigger');
  if (razorpayTriggerBtn) {
    razorpayTriggerBtn.addEventListener('click', async (e) => {
      e.preventDefault();

      const form = document.querySelector('#checkout-form');
      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }

      const payload = {
        fullName: form.fullName.value,
        phone: form.phone.value,
        streetAddress: form.streetAddress.value,
        city: form.city.value,
        state: form.state.value,
        postalCode: form.postalCode.value
      };

      const token = document.querySelector('meta[name="_csrf"]')?.content;
      const header = document.querySelector('meta[name="_csrf_header"]')?.content;
      const headers = { 'Content-Type': 'application/json' };
      if (token && header) headers[header] = token;

      try {
        razorpayTriggerBtn.disabled = true;
        razorpayTriggerBtn.textContent = 'Initiating Secure Gateway...';

        const orderRes = await fetch('/checkout/create-razorpay-order', {
          method: 'POST',
          headers,
          body: JSON.stringify(payload)
        });

        const orderData = await orderRes.json();
        if (orderData.status !== 'success') {
          alert('Could not initiate payment: ' + (orderData.message || 'Unknown error'));
          razorpayTriggerBtn.disabled = false;
          razorpayTriggerBtn.textContent = 'Pay with Razorpay';
          return;
        }

        // Open Razorpay Popup if Razorpay script loaded
        if (typeof Razorpay !== 'undefined') {
          const options = {
            key: orderData.keyId,
            amount: orderData.amount,
            currency: orderData.currency,
            name: 'StyleHub',
            description: 'Order #' + orderData.orderNumber,
            image: '/images/logo.png',
            order_id: orderData.razorpayOrderId,
            handler: async function (response) {
              // Verify signature
              const verifyRes = await fetch('/checkout/verify-payment', {
                method: 'POST',
                headers,
                body: JSON.stringify({
                  orderId: orderData.orderId,
                  razorpay_order_id: response.razorpay_order_id,
                  razorpay_payment_id: response.razorpay_payment_id,
                  razorpay_signature: response.razorpay_signature
                })
              });
              const verifyData = await verifyRes.json();
              if (verifyData.status === 'success') {
                window.location.href = '/checkout-success?orderNumber=' + verifyData.orderNumber;
              } else {
                alert('Payment verification failed.');
              }
            },
            prefill: {
              name: payload.fullName,
              contact: payload.phone
            },
            theme: { color: '#c86a4b' }
          };
          const rzp = new Razorpay(options);
          rzp.open();
        } else {
          // Fallback simulation when Razorpay checkout.js is in test/mock mode
          const proceedSimulated = confirm('Razorpay Test Mode: Simulate successful transaction for Order #' + orderData.orderNumber + '?');
          if (proceedSimulated) {
            const verifyRes = await fetch('/checkout/verify-payment', {
              method: 'POST',
              headers,
              body: JSON.stringify({
                orderId: orderData.orderId,
                razorpay_order_id: orderData.razorpayOrderId,
                razorpay_payment_id: 'pay_mock_' + Date.now(),
                razorpay_signature: 'mock_signature'
              })
            });
            const verifyData = await verifyRes.json();
            window.location.href = '/checkout-success?orderNumber=' + verifyData.orderNumber;
          } else {
            razorpayTriggerBtn.disabled = false;
            razorpayTriggerBtn.textContent = 'Pay with Razorpay';
          }
        }
      } catch (err) {
        console.error('Payment checkout error:', err);
        alert('An error occurred during payment processing.');
        razorpayTriggerBtn.disabled = false;
        razorpayTriggerBtn.textContent = 'Pay with Razorpay';
      }
    });
  }
});
